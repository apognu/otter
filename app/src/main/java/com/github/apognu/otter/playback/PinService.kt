package com.github.apognu.otter.playback

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.offline.DownloadService
import com.google.android.exoplayer2.scheduler.Scheduler
import com.google.android.exoplayer2.ui.DownloadNotificationHelper
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class PinService : DownloadService(AppContext.NOTIFICATION_DOWNLOADS) {
  private val scope: CoroutineScope = CoroutineScope(Job() + Main)

  companion object {
    fun download(context: Context, track: Track) {
      track.bestUpload()?.let { upload ->
        val url = mustNormalizeUrl(upload.listen_url)
        val data = Gson().toJson(
          DownloadInfo(
            track.id,
            url,
            track.title,
            track.artist.name,
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

    scope.launch(Main) {
      RequestBus.get().collect { request ->
        when (request) {
          is Request.GetDownloads -> request.channel?.offer(Response.Downloads(getDownloads()))
        }
      }
    }

    return super.onStartCommand(intent, flags, startId)
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

      EventBus.send(Event.DownloadChanged(download))
    }
  }
}