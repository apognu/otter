package com.github.apognu.otter.playback

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MediaControlsManager(val context: Service, private val mediaSession: MediaSessionCompat) {
  companion object {
    const val NOTIFICATION_ACTION_OPEN_QUEUE = 0
    const val NOTIFICATION_ACTION_PREVIOUS = 1
    const val NOTIFICATION_ACTION_TOGGLE = 2
    const val NOTIFICATION_ACTION_NEXT = 3
  }

  private var notification: Notification? = null

  fun updateNotification(track: Track?, playing: Boolean) {
    if (notification == null && !playing) return

    track?.let {
      val stateIcon = when (playing) {
        true -> R.drawable.pause
        false -> R.drawable.play
      }

      GlobalScope.launch(IO) {
        val openIntent = Intent(context, MainActivity::class.java).apply { action = NOTIFICATION_ACTION_OPEN_QUEUE.toString() }
        val openPendingIntent = PendingIntent.getActivity(context, 0, openIntent, 0)

        val coverUrl = maybeNormalizeUrl(track.album.cover.original)
        val cover = coverUrl?.run { Picasso.get().load(coverUrl) }

        mediaSession.setMetadata(MediaMetadataCompat.Builder().apply {
          putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist.name)
          putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
          putLong(MediaMetadata.METADATA_KEY_DURATION, (track.bestUpload()?.duration?.toLong() ?: 0L) * 1000)

          cover?.let {
            putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it.get())
          }
        }.build())

        notification = NotificationCompat.Builder(
          context,
          AppContext.NOTIFICATION_CHANNEL_MEDIA_CONTROL
        )
          .setShowWhen(false)
          .setStyle(
            MediaStyle()
              .setMediaSession(mediaSession.sessionToken)
              .setShowActionsInCompactView(0, 1, 2)
          )
          .setSmallIcon(R.drawable.ottericon)
          .run {
            if (cover != null) setLargeIcon(cover.get())
            else this
          }
          .setContentTitle(track.title)
          .setContentText(track.artist.name)
          .setContentIntent(openPendingIntent)
          .setChannelId(AppContext.NOTIFICATION_CHANNEL_MEDIA_CONTROL)
          .addAction(
            action(
              R.drawable.previous, context.getString(R.string.control_previous),
              NOTIFICATION_ACTION_PREVIOUS
            )
          )
          .addAction(
            action(
              stateIcon, context.getString(R.string.control_toggle),
              NOTIFICATION_ACTION_TOGGLE
            )
          )
          .addAction(
            action(
              R.drawable.next, context.getString(R.string.control_next),
              NOTIFICATION_ACTION_NEXT
            )
          )
          .build()

        notification?.let {
          NotificationManagerCompat.from(context).notify(AppContext.NOTIFICATION_MEDIA_CONTROL, it)
        }

        if (playing) tick()
      }
    }
  }

  fun tick() {
    notification?.let {
      context.startForeground(AppContext.NOTIFICATION_MEDIA_CONTROL, it)
    }
  }

  private fun action(icon: Int, title: String, id: Int): NotificationCompat.Action {
    val intent = Intent(context, MediaControlActionReceiver::class.java).apply { action = id.toString() }
    val pendingIntent = PendingIntent.getBroadcast(context, id, intent, 0)

    return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
  }
}

class MediaControlActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    when (intent?.action) {
      MediaControlsManager.NOTIFICATION_ACTION_PREVIOUS.toString() -> CommandBus.send(
        Command.PreviousTrack
      )
      MediaControlsManager.NOTIFICATION_ACTION_TOGGLE.toString() -> CommandBus.send(
        Command.ToggleState
      )
      MediaControlsManager.NOTIFICATION_ACTION_NEXT.toString() -> CommandBus.send(
        Command.NextTrack
      )
    }
  }
}
