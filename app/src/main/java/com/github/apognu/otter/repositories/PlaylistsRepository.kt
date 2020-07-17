package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.FunkwhalePlaylist
import com.github.apognu.otter.models.dao.DecoratedTrackEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.PlaylistEntity
import com.github.apognu.otter.models.dao.toDao
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlaylistsRepository(override val context: Context, private val database: OtterDatabase) : Repository<FunkwhalePlaylist>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/playlists/?playable=true&ordering=name", FunkwhalePlaylist.serializer())

  override fun onDataFetched(data: List<FunkwhalePlaylist>): List<FunkwhalePlaylist> {
    data.forEach {
      database.playlists().insert(it.toDao())
    }

    return super.onDataFetched(data)
  }

  fun all(): LiveData<List<PlaylistEntity>> {
    scope.launch(IO) {
      fetch().collect()
    }

    return database.playlists().all()
  }

  fun tracks(id: Int): LiveData<List<DecoratedTrackEntity>> {
    scope.launch(IO) {
      fetch().collect()
    }

    return database.playlists().tracksFor(id)
  }
}