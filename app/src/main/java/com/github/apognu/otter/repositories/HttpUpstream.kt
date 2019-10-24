package com.github.apognu.otter.repositories

import android.net.Uri
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.preference.PowerPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.Reader
import java.lang.reflect.Type
import kotlin.math.ceil

class HttpUpstream<D : Any, R : FunkwhaleResponse<D>>(private val behavior: Behavior, private val url: String, private val type: Type) : Upstream<D> {
  enum class Behavior {
    Single, AtOnce, Progressive
  }

  private var _channel: Channel<Repository.Response<D>>? = null
  private val channel: Channel<Repository.Response<D>>
    get() {
      if (_channel?.isClosedForSend ?: true) {
        _channel = Channel()
      }

      return _channel!!
    }

  override fun fetch(data: List<D>): Channel<Repository.Response<D>>? {
    if (behavior == Behavior.Single && data.isNotEmpty()) return null

    val page = ceil(data.size / AppContext.PAGE_SIZE.toDouble()).toInt() + 1

    GlobalScope.launch(Dispatchers.IO) {
      val offsetUrl =
        Uri.parse(url)
          .buildUpon()
          .appendQueryParameter("page_size", AppContext.PAGE_SIZE.toString())
          .appendQueryParameter("page", page.toString())
          .build()
          .toString()

      get(offsetUrl).fold(
        { response ->
          val data = data.plus(response.getData())

          log(data.size.toString())

          if (behavior == Behavior.Progressive || response.next == null) {
            channel.offer(Repository.Response(Repository.Origin.Network, data))
          } else {
            fetch(data)
          }
        },
        { error ->
          when (error.exception) {
            is RefreshError -> EventBus.send(Event.LogOut)
          }
        }
      )
    }

    return channel
  }

  class GenericDeserializer<T : FunkwhaleResponse<*>>(val type: Type) : ResponseDeserializable<T> {
    override fun deserialize(reader: Reader): T? {
      return Gson().fromJson(reader, type)
    }
  }

  suspend fun get(url: String): Result<R, FuelError> {
    log(url)

    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")

    val (_, response, result) = Fuel
      .get(mustNormalizeUrl(url))
      .header("Authorization", "Bearer $token")
      .awaitObjectResponseResult(GenericDeserializer<R>(type))

    if (response.statusCode == 401) {
      return retryGet(url)
    }

    return result
  }

  private suspend fun retryGet(url: String): Result<R, FuelError> {
    return if (HTTP.refresh()) {
      val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")

      Fuel
        .get(mustNormalizeUrl(url))
        .header("Authorization", "Bearer $token")
        .awaitObjectResult(GenericDeserializer(type))
    } else {
      Result.Failure(FuelError.wrap(RefreshError))
    }
  }
}