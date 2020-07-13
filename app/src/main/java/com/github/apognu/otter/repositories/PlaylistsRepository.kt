package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhalePlaylist
import com.github.apognu.otter.models.dao.toDao

class PlaylistsRepository(override val context: Context?) : Repository<FunkwhalePlaylist>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/playlists/?playable=true&ordering=name", FunkwhalePlaylist.serializer())

  override fun onDataFetched(data: List<FunkwhalePlaylist>): List<FunkwhalePlaylist> {
    data.forEach {
      Otter.get().database.playlists().insert(it.toDao())
    }

    return super.onDataFetched(data)
  }
}