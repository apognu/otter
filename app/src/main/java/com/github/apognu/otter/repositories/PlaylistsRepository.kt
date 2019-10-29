package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.Playlist
import com.github.apognu.otter.utils.PlaylistsCache
import com.github.apognu.otter.utils.PlaylistsResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class PlaylistsRepository(override val context: Context?) : Repository<Playlist, PlaylistsCache>() {
  override val cacheId = "tracks-playlists"
  override val upstream = HttpUpstream<Playlist, FunkwhaleResponse<Playlist>>(HttpUpstream.Behavior.Progressive, "/api/v1/playlists/?playable=true", object : TypeToken<PlaylistsResponse>() {}.type)

  override fun cache(data: List<Playlist>) = PlaylistsCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(PlaylistsCache::class.java).deserialize(reader)
}