package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.Favorited
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.FavoriteEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Settings
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitByteArrayResponseResult
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.stringify

class FavoritesRepository(override val context: Context, private val database: OtterDatabase) : Repository<FunkwhaleTrack>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?favorites=true&playable=true&ordering=title", FunkwhaleTrack.serializer())

  val favoritedRepository = FavoritedRepository(context, database)

  override fun onDataFetched(data: List<FunkwhaleTrack>): List<FunkwhaleTrack> = runBlocking {
    data.forEach {
      database.tracks().insertWithAssocs(database.artists(), database.albums(), database.uploads(), it)
      database.favorites().insert(FavoriteEntity(it.id))
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

  fun all(): LiveData<List<FavoriteEntity>> {
    scope.launch(IO) {
      fetch().collect()
    }

    return database.favorites().all()
  }

  fun find(ids: List<Int>) = database.albums().findAllDecorated(ids)

  fun addFavorite(id: Int) = scope.launch(IO) {
    database.favorites().add(id)

    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/")).apply {
      if (!Settings.isAnonymous()) {
        header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    scope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(AppContext.json.stringify(body))
        .awaitByteArrayResponseResult()

      favoritedRepository.update()
    }
  }

  fun deleteFavorite(id: Int) = scope.launch(IO) {
    database.favorites().remove(id)

    val body = mapOf("track" to id)

    val request = Fuel.post(mustNormalizeUrl("/api/v1/favorites/tracks/remove/")).apply {
      if (!Settings.isAnonymous()) {
        request.header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    scope.launch(IO) {
      request
        .header("Content-Type", "application/json")
        .body(AppContext.json.stringify(body))
        .awaitByteArrayResponseResult()

      favoritedRepository.update()
    }
  }
}

class FavoritedRepository(override val context: Context, private val database: OtterDatabase) : Repository<Favorited>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Single, "/api/v1/favorites/tracks/all/?playable=true", Favorited.serializer())

  override fun onDataFetched(data: List<Favorited>): List<Favorited> {
    scope.launch(IO) {
      data.forEach {
        database.favorites().insert(FavoriteEntity(it.track))
      }
    }

    return super.onDataFetched(data)
  }

  fun update() = scope.launch(IO) {
    fetch().collect()
  }
}
