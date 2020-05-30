package com.github.apognu.otter.playback

import android.content.Context
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext

data class RadioSessionBody(val radio_type: String, val custom_radio: Int)
data class RadioSession(val id: Int)
data class RadioTrackBody(val session: Int)
data class RadioTrack(val position: Int, val track: RadioTrackID)
data class RadioTrackID(val id: Int)

class RadioPlayer(val context: Context) {
  val lock = Semaphore(1)

  private var currentRadio: Radio? = null
  private var session: Int? = null

  fun play(radio: Radio) {
    currentRadio = radio
    session = null

    GlobalScope.launch(IO) {
      createSession()
    }
  }

  fun stop() {
    currentRadio = null
    session = null
  }

  fun isActive() = currentRadio != null && session != null

  private suspend fun createSession() {
      currentRadio?.let { radio ->
        try {
          val body = Gson().toJson(RadioSessionBody("custom", radio.id))
          val result = Fuel.post(mustNormalizeUrl("/api/v1/radios/sessions/"))
            .authorize()
            .header("Content-Type", "application/json")
            .body(body)
            .awaitObjectResult(gsonDeserializerOf(RadioSession::class.java))

          session = result.get().id

          prepareNextTrack(true)
        } catch (e: Exception) {
          withContext(Main) {
            context.toast(context.getString(R.string.radio_playback_error))
          }
        }
      }
  }

  suspend fun prepareNextTrack(first: Boolean = false) {
    session?.let { session ->
      try {
        val body = Gson().toJson(RadioTrackBody(session))
        val result = Fuel.post(mustNormalizeUrl("/api/v1/radios/tracks/"))
          .authorize()
          .header("Content-Type", "application/json")
          .body(body)
          .awaitObjectResult(gsonDeserializerOf(RadioTrack::class.java))

        val track = Fuel.get(mustNormalizeUrl("/api/v1/tracks/${result.get().track.id}/"))
          .authorize()
          .awaitObjectResult(gsonDeserializerOf(Track::class.java))

        if (first) {
          CommandBus.send(Command.ReplaceQueue(listOf(track.get()), true))
        } else {
          CommandBus.send(Command.AddToQueue(listOf(track.get())))
        }
      } catch (e: Exception) {
        withContext(Main) {
          context.toast(context.getString(R.string.radio_playback_error))
        }
      }
    }
  }
}