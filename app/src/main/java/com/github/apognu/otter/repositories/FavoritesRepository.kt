package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.Favorited
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.FavoriteEntity
import com.github.apognu.otter.utils.Settings
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FavoritesRepository(override val context: Context?) : Repository<FunkwhaleTrack>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?favorites=true&playable=true&ordering=title", FunkwhaleTrack.serializer())

  val favoritedRepository = FavoritedRepository(context)

  override fun onDataFetched(data: List<FunkwhaleTrack>): List<FunkwhaleTrack> = runBlocking {
    data.forEach {
      Otter.get().database.tracks().insertWithAssocs(it)
      Otter.get().database.favorites().insert(FavoriteEntity(it.id))
    }

    /* val downloaded = TracksRepository.getDownloadedIds() ?: listOf()

    data.map { track ->
      track.favorite = true
      track.downloaded = downloaded.contains(track.id)

      track.bestUpload()?.let { upload ->
        maybeNormalizeUrl(upload.listen_url)?.let { url ->
          track.cached = Otter.get().exoCache.isCached(url, 0, upload.duration * 1000L)
        }
      }

      track
    } */

    data
  }

  fun addFavorite(id: Int) = scope.launch(IO) {
    Otter.get().database.favorites().add(id)

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

      favoritedRepository.update()
    }
  }

  fun deleteFavorite(id: Int) = scope.launch(IO) {
    Otter.get().database.favorites().remove(id)

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

      favoritedRepository.update()
    }
  }
}

class FavoritedRepository(override val context: Context?) : Repository<Favorited>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Single, "/api/v1/favorites/tracks/all/?playable=true", Favorited.serializer())

  override fun onDataFetched(data: List<Favorited>): List<Favorited> {
    scope.launch(IO) {
      data.forEach {
        Otter.get().database.favorites().insert(FavoriteEntity(it.track))
      }
    }

    return super.onDataFetched(data)
  }

  fun update() = scope.launch(IO) {
    fetch().collect()
  }
}
