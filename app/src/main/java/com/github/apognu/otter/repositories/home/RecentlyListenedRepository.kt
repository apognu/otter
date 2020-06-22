package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.PlaylistTrack
import com.github.apognu.otter.utils.PlaylistTracksCache
import com.github.apognu.otter.utils.PlaylistTracksResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class RecentlyListenedRepository(override val context: Context?) : Repository<PlaylistTrack, PlaylistTracksCache>() {
  override val cacheId = "home-recently-listened"

  override val upstream =
    HttpUpstream<PlaylistTrack, FunkwhaleResponse<PlaylistTrack>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/history/listenings/?playable=true",
      object : TypeToken<PlaylistTracksResponse>() {}.type,
      10
    )

  override fun cache(data: List<PlaylistTrack>) = PlaylistTracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(PlaylistTracksCache::class.java).deserialize(reader)
}