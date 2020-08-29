package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class FavoritesRepository(override val context: Context?) : Repository<Track, TracksCache>() {
  override val cacheId = "favorites.v2"
  override val upstream = HttpUpstream<Track, OtterResponse<Track>>(context, HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?favorites=true&playable=true&ordering=title", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  private val favoritedRepository = FavoritedRepository(context)

  override fun onDataFetched(data: List<Track>): List<Track> = runBlocking {
    val downloaded = TracksRepository.getDownloadedIds() ?: listOf()

    data.map { track ->
      track.favorite = true
      track.downloaded = downloaded.contains(track.id)

      track.bestUpload()?.let { upload ->
        maybeNormalizeUrl(upload.listen_url)?.let { url ->
          track.cached = Otter.get().exoCache.isCached(url, 0, upload.duration * 1000L)
        }
      }

      track
    }
  }

  fun addFavorite(id: Int) {
    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/")).apply {
      if (!Settings.isAnonymous()) {
        header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    scope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()

      favoritedRepository.update(context, scope)
    }
  }

  fun deleteFavorite(id: Int) {
    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/remove/")).apply {
      if (!Settings.isAnonymous()) {
        request.header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    scope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(Gson().toJson(body))
        .awaitByteArrayResponseResult()

      favoritedRepository.update(context, scope)
    }
  }
}

class FavoritedRepository(override val context: Context?) : Repository<Int, FavoritedCache>() {
  override val cacheId = "favorited"
  override val upstream = HttpUpstream<Int, OtterResponse<Int>>(context, HttpUpstream.Behavior.Single, "/api/v1/favorites/tracks/all/?playable=true", object : TypeToken<FavoritedResponse>() {}.type)

  override fun cache(data: List<Int>) = FavoritedCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(FavoritedCache::class.java).deserialize(reader)

  fun update(context: Context?, scope: CoroutineScope) {
    fetch(Origin.Network.origin).untilNetwork(scope, IO) { favorites, _, _, _ ->
      Cache.set(context, cacheId, Gson().toJson(cache(favorites)).toByteArray())
    }
  }
}
