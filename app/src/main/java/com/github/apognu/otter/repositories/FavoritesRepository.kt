package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.preference.PowerPreference
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking
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
    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")
    val body = mapOf("track" to id)

    runBlocking(IO) {
      Fuel
        .post(mustNormalizeUrl("/api/v1/favorites/tracks/"))
        .header("Authorization", "Bearer $token")
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()
    }
  }

  fun deleteFavorite(id: Int) {
    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")
    val body = mapOf("track" to id)

    runBlocking(IO) {
      Fuel
        .post(mustNormalizeUrl("/api/v1/favorites/tracks/remove/"))
        .header("Authorization", "Bearer $token")
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
