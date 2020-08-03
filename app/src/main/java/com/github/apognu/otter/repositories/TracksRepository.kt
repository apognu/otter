package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.DecoratedTrackEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.utils.getMetadata
import com.github.apognu.otter.utils.maybeNormalizeUrl
import com.google.android.exoplayer2.offline.Download
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TracksRepository(override val context: Context, private val database: OtterDatabase, albumId: Int?) : Repository<FunkwhaleTrack>() {
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
      database.tracks().insertWithAssocs(database.artists(), database.albums(), database.uploads(), track)
    }

    data.sortedWith(compareBy({ it.disc_number }, { it.position }))
  }

  fun insert(track: FunkwhaleTrack) {
    database.tracks().insertWithAssocs(database.artists(), database.albums(), database.uploads(), track)
  }

  fun find(ids: List<Int>) = database.tracks().findAllDecorated(ids)

  suspend fun ofArtistBlocking(id: Int) = database.tracks().ofArtistBlocking(id)

  fun ofAlbums(albums: List<Int>): LiveData<List<DecoratedTrackEntity>> {
    scope.launch(IO) {
      fetch().collect()
    }

    return database.tracks().ofAlbumsDecorated(albums)
  }

  fun favorites() = database.tracks().favorites()

  fun isCached(track: Track): Boolean = Otter.get().exoCache.isCached(maybeNormalizeUrl(track.bestUpload()?.listen_url), 0L, track.bestUpload()?.duration?.toLong() ?: 0)

  fun downloaded(): List<Int> {
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