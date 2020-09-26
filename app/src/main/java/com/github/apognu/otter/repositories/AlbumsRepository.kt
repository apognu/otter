package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.couchbase.lite.*
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.molo17.couchbase.lite.*
import kotlinx.coroutines.GlobalScope

class AlbumsRepository(override val context: Context, private val couch: Database, artistId: Int?) : Repository<FunkwhaleAlbum>() {
  override val upstream: Upstream<FunkwhaleAlbum> by lazy {
    val url =
      if (artistId == null) "/api/v1/albums/?playable=true&ordering=title"
      else "/api/v1/albums/?playable=true&artist=$artistId&ordering=release_date"

    HttpUpstream(
      HttpUpstream.Behavior.Progressive,
      url,
      FunkwhaleAlbum.serializer()
    )
  }

  override fun onDataFetched(data: List<FunkwhaleAlbum>): List<FunkwhaleAlbum> {
    FunkwhaleAlbum.persist(couch, data)

    return super.onDataFetched(data)
  }

  fun insert(albums: List<FunkwhaleAlbum>) = FunkwhaleAlbum.persist(couch, albums)

  fun all() =
    select(SelectResult.all())
      .from(couch)
      .where { "type" equalTo "album"}
      .asFlow()
      .asLiveData()

  fun find(ids: List<Int>) =
    select(SelectResult.all())
      .from(couch)
      .where { ("type" equalTo "album") and (Meta.id.`in`(*ids.map { Expression.string("album:$it") }.toTypedArray())) }
      .asFlow()
      .asLiveData(GlobalScope.coroutineContext)

  fun ofArtist(id: Int): LiveData<ResultSet> {
    scope.launch(Dispatchers.IO) {
      fetch().collect()
    }

    return select(SelectResult.all())
      .from(couch)
      .where { ("type" equalTo "album") and ("artist_id" equalTo id) }
      .asFlow()
      .asLiveData(GlobalScope.coroutineContext)
  }
}