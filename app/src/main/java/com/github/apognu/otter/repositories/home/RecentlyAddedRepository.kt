package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.Track
import com.github.apognu.otter.utils.TracksCache
import com.github.apognu.otter.utils.TracksResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class RecentlyAddedRepository(override val context: Context?) : Repository<Track, TracksCache>() {
  override val cacheId = "home-recently-added"

  override val upstream =
    HttpUpstream<Track, FunkwhaleResponse<Track>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/tracks/?playable=true&ordering=-creation_date",
      object : TypeToken<TracksResponse>() {}.type,
      10
    )

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)
}