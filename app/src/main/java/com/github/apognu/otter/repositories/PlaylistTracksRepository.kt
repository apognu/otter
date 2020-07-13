package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhalePlaylistTrack
import com.github.apognu.otter.models.dao.PlaylistTrack
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

class PlaylistTracksRepository(override val context: Context?, private val playlistId: Int) : Repository<FunkwhalePlaylistTrack>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Single, "/api/v1/playlists/$playlistId/tracks/?playable=true", FunkwhalePlaylistTrack.serializer())

  override fun onDataFetched(data: List<FunkwhalePlaylistTrack>): List<FunkwhalePlaylistTrack> = runBlocking {
    Otter.get().database.playlists().replaceTracks(playlistId, data.map {
      Otter.get().database.tracks().insertWithAssocs(it.track)

      PlaylistTrack(playlistId, it.track.id)
    })

    data
  }
}