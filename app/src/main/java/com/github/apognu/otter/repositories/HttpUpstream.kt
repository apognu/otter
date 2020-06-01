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
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.Reader
import java.lang.reflect.Type
import kotlin.math.ceil

class HttpUpstream<D : Any, R : FunkwhaleResponse<D>>(val behavior: Behavior, private val url: String, private val type: Type) : Upstream<D> {
  enum class Behavior {
    Single, AtOnce, Progressive
  }

  override fun fetch(size: Int): Flow<Repository.Response<D>> = flow {
    if (behavior == Behavior.Single && size != 0) return@flow

    val page = ceil(size / AppContext.PAGE_SIZE.toDouble()).toInt() + 1

    val offsetUrl =
      Uri.parse(url)
        .buildUpon()
        .appendQueryParameter("page_size", AppContext.PAGE_SIZE.toString())
        .appendQueryParameter("page", page.toString())
        .build()
        .toString()

    get(offsetUrl).fold(
      { response ->
        val data = response.getData()

        if (behavior == Behavior.Progressive || response.next == null) {
          emit(Repository.Response(Repository.Origin.Network, data, false))
        } else {
          emit(Repository.Response(Repository.Origin.Network, data, true))

          fetch(size + data.size).collect { emit(it) }
        }
      },
      { error ->
        when (error.exception) {
          is RefreshError -> EventBus.send(Event.LogOut)
          else -> emit(Repository.Response(Repository.Origin.Network, listOf(), false))
        }
      }
    )
  }.flowOn(IO)

  class GenericDeserializer<T : FunkwhaleResponse<*>>(val type: Type) : ResponseDeserializable<T> {
    override fun deserialize(reader: Reader): T? {
      return Gson().fromJson(reader, type)
    }
  }

  suspend fun get(url: String): Result<R, FuelError> {
    val request = Fuel.get(mustNormalizeUrl(url)).apply {
      if (!Settings.isAnonymous()) {
        header("Authorization", "Bearer ${Settings.getAccessToken()}")
      }
    }

    val (_, response, result) = request.awaitObjectResponseResult(GenericDeserializer<R>(type))

    if (response.statusCode == 401) {
      return retryGet(url)
    }

    return result
  }

  private suspend fun retryGet(url: String): Result<R, FuelError> {
    return if (HTTP.refresh()) {
      val request = Fuel.get(mustNormalizeUrl(url)).apply {
        if (!Settings.isAnonymous()) {
          header("Authorization", "Bearer ${Settings.getAccessToken()}")
        }
      }

      request.awaitObjectResult(GenericDeserializer(type))
    } else {
      Result.Failure(FuelError.wrap(RefreshError))
    }
  }
}