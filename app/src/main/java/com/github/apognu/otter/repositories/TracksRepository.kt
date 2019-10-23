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

class TracksRepository(override val context: Context?, albumId: Int) : Repository<Track, TracksCache>() {
  override val cacheId = "tracks-album-$albumId"
  override val upstream = HttpUpstream<Track, FunkwhaleResponse<Track>>(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks?playable=true&album=$albumId", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  override fun onDataFetched(data: List<Track>): List<Track> = runBlocking {
    val favorites = FavoritedRepository(context).fetch(Origin.Network.origin).receive().data

    data.map { track ->
      track.favorite = favorites.contains(track.id)
      track
    }
  }
}