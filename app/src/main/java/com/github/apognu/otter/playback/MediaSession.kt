package com.github.apognu.otter.playback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import com.github.apognu.otter.Otter
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.CommandBus
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

class MediaSession(private val context: Context) {
  var active: Boolean = false

  private val playbackStateBuilder = PlaybackStateCompat.Builder().apply {
    setActions(
      PlaybackStateCompat.ACTION_PLAY_PAUSE or
        PlaybackStateCompat.ACTION_PLAY or
        PlaybackStateCompat.ACTION_PAUSE or
        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
        PlaybackStateCompat.ACTION_SEEK_TO or
        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
    )
  }

  val session: MediaSessionCompat by lazy {
    active = true

    MediaSessionCompat(context, context.packageName).apply {
      setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
      setPlaybackState(playbackStateBuilder.build())

      isActive = true
    }
  }

  val connector: MediaSessionConnector by lazy {
    MediaSessionConnector(Otter.get().mediaSession.session).also {
      it.setQueueNavigator(OtterQueueNavigator())

      it.setMediaButtonEventHandler { _, _, event ->
        if (!active) {
          event.extras?.getParcelable<KeyEvent>(Intent.EXTRA_KEY_EVENT)?.let { key ->
            if (key.action == KeyEvent.ACTION_UP) {
              val command = when (key.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY -> Command.ToggleState
                KeyEvent.KEYCODE_MEDIA_PAUSE -> Command.ToggleState
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Command.ToggleState
                KeyEvent.KEYCODE_MEDIA_NEXT -> Command.NextTrack
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Command.PreviousTrack
                else -> null
              }

              command?.let {
                CommandBus.send(command)
                CommandBus.send(Command.StartService(command))

                return@setMediaButtonEventHandler true
              }
            }
          }
        }

        false
      }
    }
  }
}

class OtterQueueNavigator : MediaSessionConnector.QueueNavigator {
  override fun onSkipToQueueItem(player: Player, controlDispatcher: ControlDispatcher, id: Long) {
    CommandBus.send(Command.PlayTrack(id.toInt()))
  }

  override fun onCurrentWindowIndexChanged(player: Player) {}

  override fun onCommand(player: Player, controlDispatcher: ControlDispatcher, command: String, extras: Bundle?, cb: ResultReceiver?) = true

  override fun getSupportedQueueNavigatorActions(player: Player): Long {
    return PlaybackStateCompat.ACTION_PLAY_PAUSE or
      PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
      PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
      PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
  }

  override fun onSkipToNext(player: Player, controlDispatcher: ControlDispatcher) {
    CommandBus.send(Command.NextTrack)
  }

  override fun getActiveQueueItemId(player: Player?) = player?.currentWindowIndex?.toLong() ?: 0

  override fun onSkipToPrevious(player: Player, controlDispatcher: ControlDispatcher) {
    CommandBus.send(Command.PreviousTrack)
  }

  override fun onTimelineChanged(player: Player) {}
}
