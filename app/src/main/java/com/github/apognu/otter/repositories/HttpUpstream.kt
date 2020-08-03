package com.github.apognu.otter.repositories

import android.net.Uri
import com.github.apognu.otter.models.api.OtterResponse
import com.github.apognu.otter.models.api.OtterResponseSerializer
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlin.math.ceil

class HttpUpstream<D : Any>(val behavior: Behavior, private val url: String, private val serializer: KSerializer<D>) : Upstream<D> {
  enum class Behavior {
    Single, AtOnce, Progressive
  }

  override fun fetch(size: Int): Flow<Repository.Response<D>> = flow {
    if (behavior == Behavior.Single && size != 0) return@flow

    val page = ceil(size / AppContext.PAGE_SIZE.toDouble()).toInt() + 1

    val url =
      Uri.parse(url)
        .buildUpon()
        .appendQueryParameter("page_size", AppContext.PAGE_SIZE.toString())
        .appendQueryParameter("page", page.toString())
        .appendQueryParameter("scope", Settings.getScopes().joinToString(","))
        .build()
        .toString()

    get(url).fold(
      { response ->
        val data = response.results

        when (behavior) {
          Behavior.Single -> emit(Repository.Response(data, page, false))
          Behavior.Progressive -> emit(Repository.Response(data, page, response.next != null))

          else -> {
            emit(Repository.Response(data, page, response.next != null))

            if (response.next != null) fetch(size + data.size).collect { emit(it) }
          }
        }
      },
      { error ->
        error.log()

        when (error.exception) {
          is RefreshError -> EventBus.send(Event.LogOut)
          else -> emit(Repository.Response(listOf(), page, false))
        }
      }
    )
  }.flowOn(IO)

  suspend fun get(url: String): Result<OtterResponse<D>, FuelError> {
    return try {
      val request = Fuel.get(mustNormalizeUrl(url)).apply {
        if (!Settings.isAnonymous()) {
          header("Authorization", "Bearer ${Settings.getAccessToken()}")
        }
      }

      val (_, response, result) = request.awaitObjectResponseResult(AppContext.deserializer(OtterResponseSerializer(serializer)))

      if (response.statusCode == 401) {
        return retryGet(url)
      }

      result
    } catch (e: Exception) {
      Result.error(FuelError.wrap(e))
    }
  }

  private suspend fun retryGet(url: String): Result<OtterResponse<D>, FuelError> {
    return try {
      return if (HTTP.refresh()) {
        val request = Fuel.get(mustNormalizeUrl(url)).apply {
          if (!Settings.isAnonymous()) {
            header("Authorization", "Bearer ${Settings.getAccessToken()}")
          }
        }

        request.awaitObjectResult(AppContext.deserializer(OtterResponseSerializer(serializer)))
      } else {
        Result.Failure(FuelError.wrap(RefreshError))
      }
    } catch (e: Exception) {
      Result.error(FuelError.wrap(e))
    }
  }
}