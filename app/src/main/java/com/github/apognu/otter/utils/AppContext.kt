package com.github.apognu.otter.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.apognu.otter.R
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method

object AppContext {
  const val PREFS_CREDENTIALS = "credentials"

  const val NOTIFICATION_MEDIA_CONTROL = 1
  const val NOTIFICATION_DOWNLOADS = 2
  const val NOTIFICATION_CHANNEL_MEDIA_CONTROL = "mediacontrols"
  const val NOTIFICATION_CHANNEL_DOWNLOADS = "downloads"

  const val PAGE_SIZE = 50
  const val TRANSITION_DURATION = 300L

  const val HOME_CACHE_DURATION = 15 * 60 * 1000

  fun init(context: Activity) {
    setupNotificationChannels(context)

    // CastContext.getSharedInstance(context)

    FuelManager.instance.addResponseInterceptor { next ->
      { request, response ->
        if (request.method == Method.GET && response.statusCode == 200) {
          var cacheId = request.url.path.toString()

          request.url.query?.let {
            cacheId = "$cacheId?$it"
          }

          Cache.set(context, cacheId, response.body().toByteArray())
        }

        next(request, response)
      }
    }
  }

  @SuppressLint("NewApi")
  private fun setupNotificationChannels(context: Context) {
    Build.VERSION_CODES.O.onApi {
      (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
        NotificationChannel(
          NOTIFICATION_CHANNEL_MEDIA_CONTROL,
          context.getString(R.string.playback_media_controls),
          NotificationManager.IMPORTANCE_LOW
        ).run {
          description = context.getString(R.string.playback_media_controls_description)

          enableLights(false)
          enableVibration(false)
          setSound(null, null)

          manager.createNotificationChannel(this)
        }
      }
    }

    Build.VERSION_CODES.O.onApi {
      (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).let { manager ->
        NotificationChannel(
          NOTIFICATION_CHANNEL_DOWNLOADS,
          "Downloads",
          NotificationManager.IMPORTANCE_LOW
        ).run {
          description = "Downloads"

          enableLights(false)
          enableVibration(false)
          setSound(null, null)

          manager.createNotificationChannel(this)
        }
      }
    }
  }
}

class HeadphonesUnpluggedReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    CommandBus.send(Command.SetState(false))
  }
}
