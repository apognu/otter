package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.dao.toDao

class AlbumsRepository(override val context: Context?, artistId: Int? = null) : Repository<FunkwhaleAlbum>() {
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
      Otter.get().database.albums().insert(it.toDao())
    }

    return super.onDataFetched(data)
  }
}