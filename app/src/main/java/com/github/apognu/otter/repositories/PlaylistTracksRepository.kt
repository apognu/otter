package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.FunkwhalePlaylistTrack
import com.github.apognu.otter.models.dao.DecoratedTrackEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.PlaylistTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PlaylistTracksRepository(override val context: Context?, private val database: OtterDatabase, private val playlistId: Int) : Repository<FunkwhalePlaylistTrack>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Single, "/api/v1/playlists/$playlistId/tracks/?playable=true", FunkwhalePlaylistTrack.serializer())

  override fun onDataFetched(data: List<FunkwhalePlaylistTrack>): List<FunkwhalePlaylistTrack> = runBlocking {
    database.playlists().replaceTracks(playlistId, data.map {
      database.tracks().insertWithAssocs(database.artists(), database.albums(), database.uploads(), it.track)

      PlaylistTrack(playlistId, it.track.id)
    })

    data
  }

  fun tracks(id: Int): LiveData<List<DecoratedTrackEntity>> {
    scope.launch(Dispatchers.IO) {
      fetch().collect()
    }

    return database.playlists().tracksFor(id)
  }
}