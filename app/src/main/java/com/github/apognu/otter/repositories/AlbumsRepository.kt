package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.dao.DecoratedAlbumEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.toDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AlbumsRepository(override val context: Context, private val database: OtterDatabase, artistId: Int?) : Repository<FunkwhaleAlbum>() {
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
    data.forEach {
      insert(it)
    }

    return super.onDataFetched(data)
  }

  fun insert(album: FunkwhaleAlbum) = database.albums().insert(album.toDao())
  fun all() = database.albums().allDecorated()
  fun find(ids: List<Int>) = database.albums().findAllDecorated(ids)

  fun ofArtist(id: Int): LiveData<List<DecoratedAlbumEntity>> {
    scope.launch(Dispatchers.IO) {
      fetch().collect()
    }

    return database.albums().forArtistDecorated(id)
  }
}