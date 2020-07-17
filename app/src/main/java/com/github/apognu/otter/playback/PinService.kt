package com.github.apognu.otter.playback

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.models.api.DownloadInfo
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.github.apognu.otter.viewmodels.DownloadsViewModel
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import kotlinx.serialization.stringify
import java.util.*

class PinService : DownloadService(AppContext.NOTIFICATION_DOWNLOADS) {
  companion object {
    fun download(context: Context, track: Track) {
      track.bestUpload()?.let { upload ->
        val url = mustNormalizeUrl(upload.listen_url)
        val data = AppContext.json.stringify(
          DownloadInfo(
            track.id,
            url,
            track.title,
            track.artist?.name ?: "",
            null
          )
        ).toByteArray()

        DownloadRequest(url, DownloadRequest.TYPE_PROGRESSIVE, Uri.parse(url), Collections.emptyList(), null, data).also {
          sendAddDownload(context, PinService::class.java, it, false)
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    buildResumeDownloadsIntent(this, PinService::class.java, true)

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onCreate() {
    super.onCreate()

    DownloadsViewModel.get().cursor.postValue(getDownloads())
  }

  override fun getDownloadManager() = Otter.get().exoDownloadManager.apply {
    addListener(DownloadListener())
  }

  override fun getScheduler(): Scheduler? = null

  override fun getForegroundNotification(downloads: MutableList<Download>): Notification {
    val description = resources.getQuantityString(R.plurals.downloads_description, downloads.size, downloads.size)

    return DownloadNotificationHelper(this, AppContext.NOTIFICATION_CHANNEL_DOWNLOADS).buildProgressNotification(R.drawable.downloads, null, description, downloads)
  }

  private fun getDownloads() = downloadManager.downloadIndex.getDownloads()

  inner class DownloadListener : DownloadManager.Listener {
    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download) {
      super.onDownloadChanged(downloadManager, download)

      if (download.state != Download.STATE_REMOVING) {
        EventBus.send(Event.DownloadChanged(download))
      }
    }

    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
      super.onDownloadRemoved(downloadManager, download)

      DownloadsViewModel.get().cursor.postValue(getDownloads())
    }
  }
}