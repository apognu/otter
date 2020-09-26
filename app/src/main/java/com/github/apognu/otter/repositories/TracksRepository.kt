package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.couchbase.lite.*
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.DecoratedTrackEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.utils.asLiveData
import com.github.apognu.otter.utils.getMetadata
import com.github.apognu.otter.utils.maybeNormalizeUrl
import com.google.android.exoplayer2.offline.Download
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TracksRepository(override val context: Context, private val database: OtterDatabase, private val couch: Database, albumId: Int?) : Repository<FunkwhaleTrack>() {
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
    FunkwhaleTrack.persist(couch, data)

    data.sortedWith(compareBy({ it.disc_number }, { it.position }))
  }

  fun insert(tracks: List<FunkwhaleTrack>) = FunkwhaleTrack.persist(couch, tracks)

  fun find(ids: List<Int>) = QueryBuilder
    .select(SelectResult.all())
    .from(DataSource.database(couch))
    .where(
      Expression.property("type").equalTo(Expression.string("track"))
        .and(Meta.id.`in`(*ids.map { Expression.string("track:$it") }.toTypedArray()))
    )
    .asLiveData()

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