package com.github.apognu.otter.activities

import android.os.Bundle
import kotlinx.coroutines.flow.collect
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.DownloadsAdapter
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.offline.Download
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsActivity : AppCompatActivity() {
  lateinit var adapter: DownloadsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_downloads)

    adapter = DownloadsAdapter(this, DownloadChangedListener()).also {
      downloads.layoutManager = LinearLayoutManager(this)
      downloads.adapter = it
    }
  }

  override fun onResume() {
    super.onResume()

    GlobalScope.launch(IO) {
      EventBus.get().collect { event ->
        if (event is Event.DownloadChanged) {
          refreshTrack(event.download)
        }
      }
    }

    refresh()
  }

  private fun refresh() {
    GlobalScope.launch(Main) {
      val cursor = Otter.get().exoDownloadManager.downloadIndex.getDownloads()

      adapter.downloads.clear()

      while (cursor.moveToNext()) {
        val download = cursor.download

        download.getMetadata()?.let { info ->
          adapter.downloads.add(info.apply {
            this.download = download
          })
        }
      }

      adapter.notifyDataSetChanged()
    }
  }

  private suspend fun refreshTrack(download: Download) {
    download.getMetadata()?.let { info ->
      adapter.downloads.withIndex().associate { it.value to it.index }.filter { it.key.id == info.id }.toList().getOrNull(0)?.let { match ->
        if (download.state != info.download?.state) {
          withContext(Main) {
            adapter.downloads[match.second] = info.apply {
              this.download = download
            }

            adapter.notifyItemChanged(match.second)
          }
        }
      }
    }
  }

  inner class DownloadChangedListener : DownloadsAdapter.OnDownloadChangedListener {
    override fun onItemRemoved(index: Int) {
      adapter.downloads.removeAt(index)
      adapter.notifyDataSetChanged()
    }
  }
}