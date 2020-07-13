package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.utils.getMetadata
import com.google.android.exoplayer2.offline.Download
import kotlinx.coroutines.runBlocking

class TracksRepository(override val context: Context?, albumId: Int) : Repository<FunkwhaleTrack>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&album=$albumId&ordering=disc_number,position", FunkwhaleTrack.serializer())

  companion object {
    fun getDownloadedIds(): List<Int>? {
      val cursor = Otter.get().exoDownloadManager.downloadIndex.getDownloads()
      val ids: MutableList<Int> = mutableListOf()

      while (cursor.moveToNext()) {
        val download = cursor.download

        download.getMetadata()?.let {
          if (download.state == Download.STATE_COMPLETED) {
            ids.add(it.id)
          }
        }
      }

      return ids
    }
  }

  override fun onDataFetched(data: List<FunkwhaleTrack>): List<FunkwhaleTrack> = runBlocking {
    data.forEach { track ->
      Otter.get().database.tracks().insertWithAssocs(track)
    }

    data.sortedWith(compareBy({ it.disc_number }, { it.position }))
  }
}