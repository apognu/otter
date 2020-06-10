package com.github.apognu.otter.playback

import android.app.Notification
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.AppContext
import com.google.android.exoplayer2.offline.*
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper

class PinService : DownloadService(AppContext.NOTIFICATION_DOWNLOADS) {
  private val manager by lazy {
    val database = Otter.get().exoDatabase
    val cache = Otter.get().exoCache
    val helper = DownloaderConstructorHelper(cache, QueueManager.factory(this))

    DownloadManager(this, DefaultDownloadIndex(database), DefaultDownloaderFactory(helper))
  }

  override fun getDownloadManager() = manager

  override fun getScheduler(): Scheduler? = null

  override fun getForegroundNotification(downloads: MutableList<Download>?): Notification {
    return DownloadNotificationHelper(this, AppContext.NOTIFICATION_CHANNEL_DOWNLOADS).buildProgressNotification(R.drawable.ottershape, null, null, downloads)
  }

  fun getDownloads() = manager.downloadIndex.getDownloads()
}