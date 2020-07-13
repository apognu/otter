package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.models.api.DownloadInfo
import com.github.apognu.otter.utils.getMetadata
import com.google.android.exoplayer2.offline.DownloadCursor

class DownloadsViewModel : ViewModel() {
  companion object {
    private lateinit var instance: DownloadsViewModel

    fun get(): DownloadsViewModel {
      instance = if (::instance.isInitialized) instance else DownloadsViewModel()

      return instance
    }
  }

  val cursor: MutableLiveData<DownloadCursor> by lazy { MutableLiveData<DownloadCursor>() }
  val downloads: LiveData<List<DownloadInfo>> = Transformations.map(cursor) { cursor ->
    val downloads = mutableListOf<DownloadInfo>()

    while (cursor.moveToNext()) {
      val download = cursor.download

      download.getMetadata()?.let { info ->
        downloads.add(info.apply {
          this.download = download
        })
      }
    }

    downloads.sortedBy { it.title }
  }
}