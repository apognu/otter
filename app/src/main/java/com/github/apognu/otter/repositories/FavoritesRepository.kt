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

class FavoritesRepository(override val context: Context?) : Repository<Favorite, FavoritesCache>() {
  override val cacheId = "favorites"
  override val upstream = HttpUpstream<Favorite, FunkwhaleResponse<Favorite>>(HttpUpstream.Behavior.AtOnce, "/api/v1/favorites/tracks?playable=true", object : TypeToken<FavoritesResponse>() {}.type)

  override fun cache(data: List<Favorite>) = FavoritesCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(FavoritesCache::class.java).deserialize(reader)

  override fun onDataFetched(data: List<Favorite>) = data.map {
    it.apply {
      it.track.favorite = true
    }
  }

  fun addFavorite(id: Int) {
    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")
    val body = mapOf("track" to id)

    runBlocking(IO) {
      Fuel
        .post(normalizeUrl("/api/v1/favorites/tracks"))
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
        .post(normalizeUrl("/api/v1/favorites/tracks/remove/"))
        .header("Authorization", "Bearer $token")
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()
    }
  }
}
