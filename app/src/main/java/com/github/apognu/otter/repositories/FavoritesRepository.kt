package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader

class FavoritesRepository(override val context: Context?) : Repository<Track, TracksCache>() {
  override val cacheId = "favorites.v2"
  override val upstream = HttpUpstream<Track, FunkwhaleResponse<Track>>(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?favorites=true&playable=true", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  override fun onDataFetched(data: List<Track>) = data.map {
    it.favorite = true
    it
  }

  fun addFavorite(id: Int) {
    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/")).apply {
      if (!Settings.isAnonymous()) {
        header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    GlobalScope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()
    }
  }

  fun deleteFavorite(id: Int) {
    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/remove/")).apply {
      if (!Settings.isAnonymous()) {
        request.header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    GlobalScope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()
    }
  }
}

class FavoritedRepository(override val context: Context?) : Repository<Int, FavoritedCache>() {
  override val cacheId = "favorited"
  override val upstream = HttpUpstream<Int, FunkwhaleResponse<Int>>(HttpUpstream.Behavior.Single, "/api/v1/favorites/tracks/all/?playable=true", object : TypeToken<FavoritedResponse>() {}.type)

  override fun cache(data: List<Int>) = FavoritedCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(FavoritedCache::class.java).deserialize(reader)
}
