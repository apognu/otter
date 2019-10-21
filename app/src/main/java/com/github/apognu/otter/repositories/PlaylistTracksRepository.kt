package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class PlaylistTracksRepository(override val context: Context?, playlistId: Int) : Repository<PlaylistTrack, PlaylistTracksCache>() {
  override val cacheId = "tracks-playlist-$playlistId"
  override val upstream = HttpUpstream<PlaylistTrack, FunkwhaleResponse<PlaylistTrack>>(HttpUpstream.Behavior.Single, "/api/v1/playlists/$playlistId/tracks?playable=true", object : TypeToken<PlaylistTracksResponse>() {}.type)

  override fun cache(data: List<PlaylistTrack>) = PlaylistTracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(PlaylistTracksCache::class.java).deserialize(reader)

  override fun onDataFetched(data: List<PlaylistTrack>): List<PlaylistTrack> = runBlocking {
    val favorites = FavoritesRepository(context).fetch(Origin.Network.origin).receive().data

    log(favorites.toString())

    data.map { track ->
      val favorite = favorites.find { it.track.id == track.track.id }

      if (favorite != null) {
        track.track.favorite = true
      }

      track
    }
  }
}