package com.github.apognu.otter.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.DownloadsAdapter
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.getMetadata
import com.github.apognu.otter.viewmodels.DownloadsViewModel
import com.google.android.exoplayer2.offline.Download
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsActivity : AppCompatActivity() {
  lateinit var adapter: DownloadsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_downloads)

    (downloads.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

    adapter = DownloadsAdapter(this).also {
      it.setHasStableIds(true)

      downloads.layoutManager = LinearLayoutManager(this)
      downloads.adapter = it
    }

    DownloadsViewModel.get().downloads.observe(this) { downloads ->
      adapter.downloads = downloads.toMutableList()
      adapter.notifyDataSetChanged()
    }

    lifecycleScope.launch(Default) {
      while (true) {
        delay(1000)
        refreshProgress()
      }
    }
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch(Default) {
      EventBus.get().collect { event ->
        if (event is Event.DownloadChanged) {
          refreshTrack(event.download)
        }
      }
    }
  }

  private suspend fun refreshTrack(download: Download) {
    download.getMetadata()?.let { info ->
      adapter.downloads.withIndex().associate { it.value to it.index }.filter { it.key.id == info.id }.toList().getOrNull(0)?.let { match ->
        if (download.state != info.download?.state) {
          adapter.downloads.getOrNull(match.second)?.let {
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
  }

  private suspend fun refreshProgress() {
    val cursor = Otter.get().exoDownloadManager.downloadIndex.getDownloads()

    while (cursor.moveToNext()) {
      val download = cursor.download

      download.getMetadata()?.let { info ->
        adapter.downloads.withIndex().associate { it.value to it.index }.filter { it.key.id == info.id }.toList().getOrNull(0)?.let { match ->
          if (download.state == Download.STATE_DOWNLOADING && download.percentDownloaded != info.download?.percentDownloaded ?: 0) {
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
  }
}