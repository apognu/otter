package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.Track
import com.github.apognu.otter.utils.TracksCache
import com.github.apognu.otter.utils.TracksResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class SearchRepository(override val context: Context?, query: String) : Repository<Track, TracksCache>() {
  override val cacheId: String? = null
  override val upstream = HttpUpstream<Track, FunkwhaleResponse<Track>>(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks?playable=true&q=$query", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  var query: String? = null

  override fun onDataFetched(data: List<Track>): List<Track> = runBlocking {
    val favorites = FavoritesRepository(context).fetch(Origin.Network.origin).receive().data

    data.map { track ->
      val favorite = favorites.find { it.track.id == track.id }

      if (favorite != null) {
        track.favorite = true
      }

      track
    }
  }
}