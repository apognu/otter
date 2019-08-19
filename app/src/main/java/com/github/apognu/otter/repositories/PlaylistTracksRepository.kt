package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.PlaylistTrack
import com.github.apognu.otter.utils.PlaylistTracksCache
import com.github.apognu.otter.utils.PlaylistTracksResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class PlaylistTracksRepository(override val context: Context?, playlistId: Int) : Repository<PlaylistTrack, PlaylistTracksCache>() {
  override val cacheId = "tracks-playlist-$playlistId"
  override val upstream = HttpUpstream<PlaylistTrack, FunkwhaleResponse<PlaylistTrack>>(HttpUpstream.Behavior.AtOnce, "/api/v1/playlists/$playlistId/tracks?playable=true", object : TypeToken<PlaylistTracksResponse>() {}.type)

  override fun cache(data: List<PlaylistTrack>) = PlaylistTracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(PlaylistTracksCache::class.java).deserialize(reader)
}