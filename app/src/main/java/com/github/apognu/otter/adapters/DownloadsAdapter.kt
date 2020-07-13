package com.github.apognu.otter.adapters

import android.content.Context
import android.graphics.drawable.Icon
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.models.api.DownloadInfo
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.playback.PinService
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.offline.DownloadService
import kotlinx.android.synthetic.main.row_download.view.*

class DownloadsAdapter(private val context: Context) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {
  var downloads: MutableList<DownloadInfo> = mutableListOf()

  override fun getItemId(position: Int) = downloads[position].id.toLong()

  override fun getItemCount() = downloads.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_download, parent, false)

    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val download = downloads[position]

    holder.title.text = download.title
    holder.artist.text = download.artist

    download.download?.let { state ->
      when (state.isTerminalState) {
        true -> {
          holder.progress.visibility = View.INVISIBLE

          when (state.state) {
            Download.STATE_FAILED -> {
              holder.toggle.setImageDrawable(context.getDrawable(R.drawable.retry))
              holder.progress.visibility = View.INVISIBLE
            }

            else -> holder.toggle.visibility = View.GONE
          }
        }

        false -> {
          holder.progress.visibility = View.VISIBLE
          holder.toggle.visibility = View.VISIBLE
          holder.progress.isIndeterminate = false
          holder.progress.progress = state.percentDownloaded.toInt()

          when (state.state) {
            Download.STATE_QUEUED -> {
              holder.progress.isIndeterminate = true
            }

            Download.STATE_REMOVING -> {
              holder.progress.visibility = View.GONE
              holder.toggle.visibility = View.GONE
            }

            Download.STATE_STOPPED -> holder.toggle.setImageIcon(Icon.createWithResource(context, R.drawable.play))

            else -> holder.toggle.setImageIcon(Icon.createWithResource(context, R.drawable.pause))
          }
        }
      }

      holder.toggle.setOnClickListener {
        when (state.state) {
          Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> DownloadService.sendSetStopReason(context, PinService::class.java, download.contentId, 1, false)

          Download.STATE_FAILED -> {
            FunkwhaleTrack.fromDownload(download).also {
              // PinService.download(context, it)
            }
          }

          else -> DownloadService.sendSetStopReason(context, PinService::class.java, download.contentId, Download.STOP_REASON_NONE, false)
        }
      }

      holder.delete.setOnClickListener {
        DownloadService.sendRemoveDownload(context, PinService::class.java, download.contentId, false)
      }
    }
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.title
    val artist = view.artist
    val progress = view.progress
    val toggle = view.toggle
    val delete = view.delete
  }
}