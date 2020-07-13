package com.github.apognu.otter.playback

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.RadioEntity
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritedRepository
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class RadioSessionBody(val radio_type: String?, var custom_radio: Int? = null, var related_object_id: String? = null)

@Serializable
data class RadioSession(val id: Int)

@Serializable
data class RadioTrackBody(val session: Int)

@Serializable
data class RadioTrack(val position: Int, val track: RadioTrackID)

@Serializable
data class RadioTrackID(val id: Int)

class RadioPlayer(val context: Context, val scope: CoroutineScope) {
  val lock = Semaphore(1)

  private var currentRadio: RadioEntity? = null
  private var session: Int? = null
  private var cookie: String? = null

  private val favoritedRepository = FavoritedRepository(context)

  init {
    Cache.get(context, "radio_type")?.readLine()?.let { radio_type ->
      Cache.get(context, "radio_id")?.readLine()?.toInt()?.let { radio_id ->
        Cache.get(context, "radio_session")?.readLine()?.toInt()?.let { radio_session ->
          val cachedCookie = Cache.get(context, "radio_cookie")?.readLine()

          currentRadio = RadioEntity(radio_id, radio_type, "", "")
          session = radio_session
          cookie = cachedCookie
        }
      }
    }
  }

  fun play(radio: RadioEntity) {
    currentRadio = radio
    session = null

    scope.launch(IO) {
      createSession()
    }
  }

  fun stop() {
    currentRadio = null
    session = null

    Cache.delete(context, "radio_type")
    Cache.delete(context, "radio_id")
    Cache.delete(context, "radio_session")
    Cache.delete(context, "radio_cookie")
  }

  fun isActive() = currentRadio != null && session != null

  private suspend fun createSession() {
    "createSession".log()

    currentRadio?.let { radio ->
      try {
        val request = RadioSessionBody(radio.radio_type, related_object_id = radio.related_object_id).apply {
          if (radio_type == "custom") {
            custom_radio = radio.id
          }
        }

        val body = Gson().toJson(request)
        val (_, response, result) = Fuel.post(mustNormalizeUrl("/api/v1/radios/sessions/"))
          .authorize()
          .header("Content-Type", "application/json")
          .body(body)
          .awaitObjectResponseResult<RadioSession>(AppContext.deserializer())

        session = result.get().id
        cookie = response.header("set-cookie").joinToString(";")

        radio.radio_type?.let { type -> Cache.set(context, "radio_type", type.toByteArray()) }
        Cache.set(context, "radio_id", radio.id.toString().toByteArray())
        Cache.set(context, "radio_session", session.toString().toByteArray())
        Cache.set(context, "radio_cookie", cookie.toString().toByteArray())

        prepareNextTrack(true)
      } catch (e: Exception) {
        e.log()

        withContext(Main) {
          context.toast(context.getString(R.string.radio_playback_error))
        }
      }
    }
  }

  suspend fun prepareNextTrack(first: Boolean = false) {
    "prepareTrack".log()

    session?.let { session ->
      try {
        val body = Gson().toJson(RadioTrackBody(session))
        val result = Fuel.post(mustNormalizeUrl("/api/v1/radios/tracks/"))
          .authorize()
          .header("Content-Type", "application/json")
          .apply {
            cookie?.let {
              header("cookie", it)
            }
          }
          .body(body)
          .awaitObjectResult<RadioTrack>(AppContext.deserializer())

        val track = Fuel.get(mustNormalizeUrl("/api/v1/tracks/${result.get().track.id}/"))
          .authorize()
          .awaitObjectResult<FunkwhaleTrack>(AppContext.deserializer())
          .get()

        Otter.get().database.tracks().run {
          insertWithAssocs(track)

          Track.fromDecoratedEntity(find(track.id)).let { track ->
            if (first) {
              CommandBus.send(Command.ReplaceQueue(listOf(track), true))
            } else {
              CommandBus.send(Command.AddToQueue(listOf(track)))
            }
          }
        }
      } catch (e: Exception) {
        withContext(Main) {
          context.toast(context.getString(R.string.radio_playback_error))
        }
      } finally {
        EventBus.send(Event.RadioStarted)
      }
    }
  }
}