package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.OtterResponse
import com.github.apognu.otter.utils.PlaylistTrack
import com.github.apognu.otter.utils.PlaylistTracksCache
import com.github.apognu.otter.utils.PlaylistTracksResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class PlaylistTracksRepository(override val context: Context?, playlistId: Int) : Repository<PlaylistTrack, PlaylistTracksCache>() {
  override val cacheId = "tracks-playlist-$playlistId"
  override val upstream = HttpUpstream<PlaylistTrack, OtterResponse<PlaylistTrack>>(context, HttpUpstream.Behavior.Single, "/api/v1/playlists/$playlistId/tracks/?playable=true", object : TypeToken<PlaylistTracksResponse>() {}.type)

  override fun cache(data: List<PlaylistTrack>) = PlaylistTracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(PlaylistTracksCache::class.java).deserialize(reader)

  override fun onDataFetched(data: List<PlaylistTrack>): List<PlaylistTrack> = runBlocking {
    val favorites = FavoritedRepository(context).fetch(Origin.Network.origin)
      .map { it.data }
      .toList()
      .flatten()

    data.map { track ->
      track.track.favorite = favorites.contains(track.track.id)
      track
    }
  }
}