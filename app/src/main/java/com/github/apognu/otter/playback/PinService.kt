package com.github.apognu.otter.playback

import android.app.Notification
import android.content.Intent
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PinService : DownloadService(AppContext.NOTIFICATION_DOWNLOADS) {
  private val manager by lazy {
    val database = Otter.get().exoDatabase
    val cache = Otter.get().exoCache
    val helper = DownloaderConstructorHelper(cache, QueueManager.factory(this))

    DownloadManager(this, DefaultDownloadIndex(database), DefaultDownloaderFactory(helper))
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    buildResumeDownloadsIntent(this, PinService::class.java, true)

    GlobalScope.launch(Main) {
      RequestBus.get().collect { request ->
        when (request) {
          is Request.GetDownloads -> request.channel?.offer(Response.Downloads(getDownloads()))
        }
      }
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun getDownloadManager() = manager

  override fun getScheduler(): Scheduler? = null

  override fun getForegroundNotification(downloads: MutableList<Download>?): Notification {
    val quantity = downloads?.size ?: 0
    val description = resources.getQuantityString(R.plurals.downloads_description, quantity, quantity)

    return DownloadNotificationHelper(this, AppContext.NOTIFICATION_CHANNEL_DOWNLOADS).buildProgressNotification(R.drawable.downloads, null, description, downloads)
  }

  override fun onDownloadChanged(download: Download?) {
    super.onDownloadChanged(download)

    EventBus.send(Event.DownloadChanged)
  }

  private fun getDownloads() = manager.downloadIndex.getDownloads()
}