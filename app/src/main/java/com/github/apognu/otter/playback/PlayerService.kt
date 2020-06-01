package com.github.apognu.otter.playback

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlayerService : Service() {
  private lateinit var queue: QueueManager
  private val jobs = mutableListOf<Job>()

  private lateinit var audioManager: AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null
  private val audioFocusChangeListener = AudioFocusChange()
  private var stateWhenLostFocus = false

  private lateinit var mediaControlsManager: MediaControlsManager
  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var player: SimpleExoPlayer

  private lateinit var playerEventListener: PlayerEventListener
  private val headphonesUnpluggedReceiver = HeadphonesUnpluggedReceiver()

  private var progressCache = Triple(0, 0, 0)

  private lateinit var radioPlayer: RadioPlayer

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    watchEventBus()

    return START_STICKY
  }

  override fun onCreate() {
    super.onCreate()

    queue = QueueManager(this)
    radioPlayer = RadioPlayer(this)

    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
        setAudioAttributes(AudioAttributes.Builder().run {
          setUsage(AudioAttributes.USAGE_MEDIA)
          setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

          setAcceptsDelayedFocusGain(true)
          setOnAudioFocusChangeListener(audioFocusChangeListener)

          build()
        })

        build()
      }
    }

    mediaSession = MediaSessionCompat(this, applicationContext.packageName).apply {
      isActive = true
    }

    mediaControlsManager = MediaControlsManager(this, mediaSession)

    player = ExoPlayerFactory.newSimpleInstance(this).apply {
      playWhenReady = false

      playerEventListener = PlayerEventListener().also {
        addListener(it)
      }

      MediaSessionConnector(mediaSession).also {
        it.setPlayer(this)
        it.setMediaButtonEventHandler { player, _, mediaButtonEvent ->
          mediaButtonEvent?.extras?.getParcelable<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.let { key ->
            if (key.action == KeyEvent.ACTION_UP) {
              when (key.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> state(true)
                KeyEvent.KEYCODE_MEDIA_PAUSE -> state(false)
                KeyEvent.KEYCODE_MEDIA_NEXT -> player?.next()
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> previousTrack()
              }
            }
          }

          true
        }
      }
    }

    if (queue.current > -1) {
      player.prepare(queue.datasources, true, true)

      Cache.get(this, "progress")?.let { progress ->
        player.seekTo(queue.current, progress.readLine().toLong())

        val (current, duration, percent) = progress(true)

        ProgressBus.send(current, duration, percent)
      }
    }

    registerReceiver(headphonesUnpluggedReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
  }

  private fun watchEventBus() {
    jobs.add(GlobalScope.launch(Main) {
      for (message in CommandBus.get()) {
        when (message) {
          is Command.RefreshService -> {
            EventBus.send(Event.QueueChanged)

            if (queue.metadata.isNotEmpty()) {
              EventBus.send(Event.RefreshTrack(queue.current(), player.playWhenReady))
              EventBus.send(Event.StateChanged(player.playWhenReady))
            }
          }

          is Command.ReplaceQueue -> {
            if (!message.fromRadio) radioPlayer.stop()

            queue.replace(message.queue)
            player.prepare(queue.datasources, true, true)

            state(true)

            EventBus.send(
              Event.RefreshTrack(
                queue.current(),
                true
              )
            )
          }

          is Command.AddToQueue -> queue.append(message.tracks)
          is Command.PlayNext -> queue.insertNext(message.track)
          is Command.RemoveFromQueue -> queue.remove(message.track)
          is Command.MoveFromQueue -> queue.move(message.oldPosition, message.newPosition)

          is Command.PlayTrack -> {
            queue.current = message.index
            player.seekTo(message.index, C.TIME_UNSET)

            state(true)

            EventBus.send(Event.RefreshTrack(queue.current(),true))
          }

          is Command.ToggleState -> toggle()
          is Command.SetState -> state(message.state)

          is Command.NextTrack -> {
            player.next()

            Cache.set(this@PlayerService, "progress", "0".toByteArray())
            ProgressBus.send(0, 0, 0)
          }
          is Command.PreviousTrack -> previousTrack()
          is Command.Seek -> progress(message.progress)

          is Command.ClearQueue -> queue.clear()

          is Command.PlayRadio -> {
            queue.clear()
            radioPlayer.play(message.radio)
          }

          is Command.SetRepeatMode -> player.repeatMode = message.mode
        }

        if (player.playWhenReady) {
          mediaControlsManager.tick()
        }
      }
    })

    jobs.add(GlobalScope.launch(Main) {
      RequestBus.get().collect { request ->
        when (request) {
          is Request.GetCurrentTrack -> request.channel?.offer(Response.CurrentTrack(queue.current()))
          is Request.GetState -> request.channel?.offer(Response.State(player.playWhenReady))
          is Request.GetQueue -> request.channel?.offer(Response.Queue(queue.get()))
        }
      }
    })

    jobs.add(GlobalScope.launch(Main) {
      while (true) {
        delay(1000)

        val (current, duration, percent) = progress()

        if (player.playWhenReady) {
          ProgressBus.send(current, duration, percent)
        }
      }
    })
  }

  override fun onBind(intent: Intent?): IBinder? = null

  @SuppressLint("NewApi")
  override fun onDestroy() {
    jobs.forEach { it.cancel() }

    try {
      unregisterReceiver(headphonesUnpluggedReceiver)
    } catch (_: Exception) {
    }

    Build.VERSION_CODES.O.onApi(
      {
        audioFocusRequest?.let {
          audioManager.abandonAudioFocusRequest(it)
        }
      },
      {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(audioFocusChangeListener)
      })

    mediaSession.isActive = false
    mediaSession.release()

    player.removeListener(playerEventListener)
    state(false)
    player.release()

    queue.cache.release()

    stopForeground(true)
    stopSelf()

    super.onDestroy()
  }

  @SuppressLint("NewApi")
  private fun state(state: Boolean) {
    if (!state) {
      val (progress, _, _) = progress()

      Cache.set(this@PlayerService,"progress", progress.toString().toByteArray())
    }

    if (state && player.playbackState == Player.STATE_IDLE) {
      player.prepare(queue.datasources)
    }

    var allowed = !state

    if (!allowed) {
      Build.VERSION_CODES.O.onApi(
        {
          audioFocusRequest?.let {
            allowed = when (audioManager.requestAudioFocus(it)) {
              AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
              else -> false
            }
          }
        },
        {

          @Suppress("DEPRECATION")
          audioManager.requestAudioFocus(audioFocusChangeListener, AudioAttributes.CONTENT_TYPE_MUSIC, AudioManager.AUDIOFOCUS_GAIN).let {
            allowed = when (it) {
              AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> true
              else -> false
            }
          }
        }
      )
    }

    if (allowed) {
      player.playWhenReady = state

      EventBus.send(Event.StateChanged(state))
    }
  }

  private fun toggle() {
    state(!player.playWhenReady)
  }

  private fun previousTrack() {
    if (player.currentPosition > 5000) {
      return player.seekTo(0)
    }

    player.previous()
  }

  private fun progress(force: Boolean = false): Triple<Int, Int, Int> {
    if (!player.playWhenReady && !force) return progressCache

    return queue.current()?.bestUpload()?.let { upload ->
      val current = player.currentPosition
      val duration = upload.duration.toFloat()
      val percent = ((current / (duration * 1000)) * 100).toInt()

      progressCache = Triple(current.toInt(), duration.toInt(), percent)
      progressCache
    } ?: Triple(0, 0, 0)
  }

  private fun progress(value: Int) {
    val duration = ((queue.current()?.bestUpload()?.duration ?: 0) * (value.toFloat() / 100)) * 1000

    progressCache = Triple(duration.toInt(), queue.current()?.bestUpload()?.duration ?: 0, value)

    player.seekTo(duration.toLong())
  }

  inner class PlayerEventListener : Player.EventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      super.onPlayerStateChanged(playWhenReady, playbackState)

      EventBus.send(Event.StateChanged(playWhenReady))

      if (queue.current == -1) {
        EventBus.send(Event.TrackPlayed(queue.current(), playWhenReady))
      }

      when (playWhenReady) {
        true -> {
          when (playbackState) {
            Player.STATE_READY -> mediaControlsManager.updateNotification(queue.current(), true)
            Player.STATE_BUFFERING -> EventBus.send(Event.Buffering(true))
            Player.STATE_IDLE -> state(false)
            Player.STATE_ENDED -> EventBus.send(Event.PlaybackStopped)
          }

          if (playbackState != Player.STATE_BUFFERING) EventBus.send(Event.Buffering(false))
        }

        false -> {
          EventBus.send(Event.StateChanged(false))
          EventBus.send(Event.Buffering(false))

          if (playbackState == Player.STATE_READY) {
            mediaControlsManager.updateNotification(queue.current(), false)
            stopForeground(false)
          }
        }
      }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
      super.onTracksChanged(trackGroups, trackSelections)

      queue.current = player.currentWindowIndex
      mediaControlsManager.updateNotification(queue.current(), player.playWhenReady)

      if (queue.get().isNotEmpty() && queue.current() == queue.get().last() && radioPlayer.isActive()) {
        GlobalScope.launch(IO) {
          if (radioPlayer.lock.tryAcquire()) {
            radioPlayer.prepareNextTrack()
            radioPlayer.lock.release()
          }
        }
      }

      Cache.set(this@PlayerService,"current", queue.current.toString().toByteArray()      )

      EventBus.send(Event.RefreshTrack(queue.current(),true)      )
    }

    override fun onPositionDiscontinuity(reason: Int) {
      super.onPositionDiscontinuity(reason)

      if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
        EventBus.send(Event.TrackFinished(queue.current()))
      }
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
      EventBus.send(Event.PlaybackError(getString(R.string.error_playback)))

      player.next()
      player.playWhenReady = true
    }
  }

  inner class AudioFocusChange : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focus: Int) {
      when (focus) {
        AudioManager.AUDIOFOCUS_GAIN -> {
          player.volume = 1f

          state(stateWhenLostFocus)
          stateWhenLostFocus = false
        }

        AudioManager.AUDIOFOCUS_LOSS -> {
          stateWhenLostFocus = false
          state(false)
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          stateWhenLostFocus = player.playWhenReady
          state(false)
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          stateWhenLostFocus = player.playWhenReady
          player.volume = 0.3f
        }
      }
    }
  }
}
