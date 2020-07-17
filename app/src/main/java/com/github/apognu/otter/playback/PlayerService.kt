package com.github.apognu.otter.playback

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.viewmodels.PlayerStateViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.android.inject

class PlayerService : Service() {
  private val playerViewModel by inject<PlayerStateViewModel>()

  companion object {
    const val INITIAL_COMMAND_KEY = "start_command"
  }

  private var started = false
  private val scope: CoroutineScope = CoroutineScope(Job() + Main)

  private lateinit var audioManager: AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null
  private val audioFocusChangeListener = AudioFocusChange()
  private var stateWhenLostFocus = false

  private lateinit var queue: QueueManager
  private lateinit var mediaControlsManager: MediaControlsManager
  private lateinit var player: SimpleExoPlayer

  private val mediaMetadataBuilder = MediaMetadataCompat.Builder()

  private lateinit var playerEventListener: PlayerEventListener
  private val headphonesUnpluggedReceiver = HeadphonesUnpluggedReceiver()

  private var progressCache = Triple(0, 0, 0)

  private lateinit var radioPlayer: RadioPlayer

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    intent?.action?.let {
      if (it == Intent.ACTION_MEDIA_BUTTON) {
        intent.extras?.getParcelable<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.let { key ->
          when (key.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
              if (hasAudioFocus(true)) MediaButtonReceiver.handleIntent(Otter.get().mediaSession.session, intent)
              Unit
            }
            else -> MediaButtonReceiver.handleIntent(Otter.get().mediaSession.session, intent)
          }
        }
      }
    }

    if (!started) {
      watchEventBus()
    }

    started = true

    return START_STICKY
  }

  @SuppressLint("NewApi")
  override fun onCreate() {
    super.onCreate()

    queue = QueueManager(this)
    radioPlayer = RadioPlayer(this, scope)

    audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

    Build.VERSION_CODES.O.onApi {
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

    mediaControlsManager = MediaControlsManager(this, scope, Otter.get().mediaSession.session)

    player = SimpleExoPlayer.Builder(this).build().apply {
      playWhenReady = false

      playerEventListener = PlayerEventListener().also {
        addListener(it)
      }
    }

    Otter.get().mediaSession.active = true

    Otter.get().mediaSession.connector.apply {
      setPlayer(player)

      setMediaMetadataProvider {
        buildTrackMetadata(queue.current())
      }
    }

    if (queue.current > -1) {
      player.prepare(queue.datasources)

      Cache.get(this, "progress")?.let { progress ->
        player.seekTo(queue.current, progress.readLine().toLong())

        val (current, duration, percent) = getProgress(true)

        playerViewModel.position.postValue(Triple(current, duration, percent))
      }
    }

    registerReceiver(headphonesUnpluggedReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
  }

  private fun watchEventBus() {
    scope.launch(Main) {
      CommandBus.get().collect { command ->
        when (command) {
          is Command.RefreshService -> {
            if (queue.metadata.isNotEmpty()) {
              playerViewModel._track.postValue(queue.current())
              playerViewModel.isPlaying.postValue(player.playWhenReady)
            }
          }

          is Command.ReplaceQueue -> {
            if (!command.fromRadio) radioPlayer.stop()

            queue.replace(command.queue)
            player.prepare(queue.datasources, true, true)

            setPlaybackState(true)

            CommandBus.send(Command.RefreshTrack(queue.current()))
          }

          is Command.AddToQueue -> queue.append(command.tracks)
          is Command.PlayNext -> queue.insertNext(command.track)
          is Command.RemoveFromQueue -> queue.remove(command.track)
          is Command.MoveFromQueue -> queue.move(command.oldPosition, command.newPosition)

          is Command.PlayTrack -> {
            queue.current = command.index
            player.seekTo(command.index, C.TIME_UNSET)

            setPlaybackState(true)

            CommandBus.send(Command.RefreshTrack(queue.current()))
          }

          is Command.ToggleState -> togglePlayback()
          is Command.SetState -> setPlaybackState(command.state)

          is Command.NextTrack -> skipToNextTrack()
          is Command.PreviousTrack -> skipToPreviousTrack()
          is Command.Seek -> seek(command.progress)

          is Command.ClearQueue -> {
            queue.clear()
            player.stop()
          }
          is Command.ShuffleQueue -> queue.shuffle()

          is Command.PlayRadio -> {
            queue.clear()
            // TOBEREDONE
            radioPlayer.play(command.radio)
          }

          is Command.SetRepeatMode -> player.repeatMode = command.mode

          is Command.PinTrack -> PinService.download(this@PlayerService, command.track)
          is Command.PinTracks -> command.tracks.forEach { PinService.download(this@PlayerService, it) }
        }
      }
    }

    scope.launch(Main) {
      while (true) {
        delay(1000)

        if (player.playWhenReady) {
          playerViewModel.position.postValue(getProgress())
        }
      }
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)

    if (!player.playWhenReady) {
      NotificationManagerCompat.from(this).cancelAll()
      stopSelf()
    }
  }

  @SuppressLint("NewApi")
  override fun onDestroy() {
    scope.cancel()

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

    player.removeListener(playerEventListener)
    setPlaybackState(false)
    player.release()

    Otter.get().mediaSession.active = false

    super.onDestroy()
  }

  private fun setPlaybackState(state: Boolean) {
    if (!state) {
      val (progress, _, _) = getProgress()

      Cache.set(this@PlayerService, "progress", progress.toString().toByteArray())
    }

    if (state && player.playbackState == Player.STATE_IDLE) {
      player.prepare(queue.datasources)
    }

    if (hasAudioFocus(state)) {
      player.playWhenReady = state

      playerViewModel.isPlaying.postValue(state)
    }
  }

  private fun togglePlayback() {
    setPlaybackState(!player.playWhenReady)
  }

  private fun skipToPreviousTrack() {
    if (player.currentPosition > 5000) {
      return player.seekTo(0)
    }

    player.previous()
  }

  private fun skipToNextTrack() {
    player.next()

    Cache.set(this@PlayerService, "progress", "0".toByteArray())
    playerViewModel.position.postValue(Triple(0, 0, 0))
  }

  private fun getProgress(force: Boolean = false): Triple<Int, Int, Int> {
    if (!player.playWhenReady && !force) return progressCache

    return queue.current()?.bestUpload()?.let { upload ->
      val current = player.currentPosition
      val duration = upload.duration.toFloat()
      val percent = ((current / (duration * 1000)) * 100).toInt()

      progressCache = Triple(current.toInt(), duration.toInt(), percent)
      progressCache
    } ?: Triple(0, 0, 0)
  }

  private fun seek(value: Int) {
    val duration = ((queue.current()?.bestUpload()?.duration ?: 0) * (value.toFloat() / 100)) * 1000

    progressCache = Triple(duration.toInt(), queue.current()?.bestUpload()?.duration ?: 0, value)

    player.seekTo(duration.toLong())
  }

  private fun buildTrackMetadata(track: Track?): MediaMetadataCompat {
    track?.let {
      val coverUrl = maybeNormalizeUrl(track.album?.cover())

      return mediaMetadataBuilder.apply {
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist?.name)
        putLong(MediaMetadata.METADATA_KEY_DURATION, (track.bestUpload()?.duration?.toLong() ?: 0L) * 1000)

        try {
          runBlocking(IO) {
            this@apply.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, Picasso.get().load(coverUrl).get())
          }
        } catch (e: Exception) {
        }
      }.build()
    }

    return mediaMetadataBuilder.build()
  }

  @SuppressLint("NewApi")
  private fun hasAudioFocus(state: Boolean): Boolean {
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

    return allowed
  }

  @SuppressLint("NewApi")
  inner class PlayerEventListener : Player.EventListener {
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
      super.onPlayerStateChanged(playWhenReady, playbackState)

      playerViewModel.isPlaying.postValue(playWhenReady)

      if (queue.current == -1) {
        playerViewModel._track.postValue(queue.current())
      }

      when (playWhenReady) {
        true -> {
          when (playbackState) {
            Player.STATE_READY -> mediaControlsManager.updateNotification(queue.current(), true)
            Player.STATE_BUFFERING -> playerViewModel.isBuffering.postValue(true)
            Player.STATE_ENDED -> {
              setPlaybackState(false)

              queue.current = 0
              player.seekTo(0, C.TIME_UNSET)
            }

            Player.STATE_IDLE -> {
              setPlaybackState(false)

              return EventBus.send(Event.PlaybackStopped)
            }
          }

          if (playbackState != Player.STATE_BUFFERING) playerViewModel.isBuffering.postValue(false)
        }

        false -> {
          playerViewModel.isBuffering.postValue(false)

          Build.VERSION_CODES.N.onApi(
            { stopForeground(STOP_FOREGROUND_DETACH) },
            { stopForeground(false) }
          )

          when (playbackState) {
            Player.STATE_READY -> mediaControlsManager.updateNotification(queue.current(), false)
            Player.STATE_IDLE -> mediaControlsManager.remove()
          }
        }
      }
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
      super.onTracksChanged(trackGroups, trackSelections)

      if (queue.current != player.currentWindowIndex) {
        queue.current = player.currentWindowIndex
        mediaControlsManager.updateNotification(queue.current(), player.playWhenReady)
      }

      if (queue.get().isNotEmpty() && queue.current() == queue.get().last() && radioPlayer.isActive()) {
        scope.launch(IO) {
          if (radioPlayer.lock.tryAcquire()) {
            radioPlayer.prepareNextTrack()
            radioPlayer.lock.release()
          }
        }
      }

      Cache.set(this@PlayerService, "current", queue.current.toString().toByteArray())

      playerViewModel._track.postValue(queue.current())
    }

    override fun onPositionDiscontinuity(reason: Int) {
      super.onPositionDiscontinuity(reason)

      if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
        EventBus.send(Event.TrackFinished(queue.current()))
      }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
      EventBus.send(Event.PlaybackError(getString(R.string.error_playback)))

      if (player.playWhenReady) {
        queue.current++
        player.prepare(queue.datasources, true, true)
        player.seekTo(queue.current, 0)

        CommandBus.send(Command.RefreshTrack(queue.current()))
      }
    }
  }

  inner class AudioFocusChange : AudioManager.OnAudioFocusChangeListener {
    override fun onAudioFocusChange(focus: Int) {
      when (focus) {
        AudioManager.AUDIOFOCUS_GAIN -> {
          player.volume = 1f

          setPlaybackState(stateWhenLostFocus)
          stateWhenLostFocus = false
        }

        AudioManager.AUDIOFOCUS_LOSS -> {
          stateWhenLostFocus = false
          setPlaybackState(false)
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          stateWhenLostFocus = player.playWhenReady
          setPlaybackState(false)
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          stateWhenLostFocus = player.playWhenReady
          player.volume = 0.3f
        }
      }
    }
  }
}