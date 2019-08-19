package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.CacheItem
import com.github.apognu.otter.utils.untilNetwork
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import java.io.BufferedReader

interface Upstream<D> {
  fun fetch(data: List<D> = listOf()): Channel<Repository.Response<D>>?
}

abstract class Repository<D : Any, C : CacheItem<D>> {
  enum class Origin(val origin: Int) {
    Cache(0b01),
    Network(0b10)
  }

  data class Response<D>(val origin: Origin, val data: List<D>)

  abstract val context: Context?
  abstract val cacheId: String?
  abstract val upstream: Upstream<D>

  private var _channel: Channel<Response<D>>? = null
  private val channel: Channel<Response<D>>
    get() {
      if (_channel?.isClosedForSend ?: true) {
        _channel = Channel(10)
      }

      return _channel!!
    }

  protected open fun cache(data: List<D>): C? = null
  protected open fun uncache(reader: BufferedReader): C? = null

  fun fetch(upstreams: Int = Origin.Cache.origin and Origin.Network.origin, from: List<D> = listOf()): Channel<Response<D>> {
    if (Origin.Cache.origin and upstreams == upstreams) fromCache()
    if (Origin.Network.origin and upstreams == upstreams) fromNetwork(from)

    return channel
  }

  private fun fromCache() {
    cacheId?.let { cacheId ->
      Cache.get(context, cacheId)?.let { reader ->
        uncache(reader)?.let { cache ->
          channel.offer(Response(Origin.Cache, cache.data))
        }
      }
    }
  }

  private fun fromNetwork(from: List<D>) {
    upstream.fetch(data = from)?.untilNetwork(IO) {
      val data = onDataFetched(it)

      cacheId?.let { cacheId ->
        Cache.set(
          context,
          cacheId,
          Gson().toJson(cache(data)).toByteArray()
        )
      }

      channel.offer(Response(Origin.Network, data))
    }
  }

  protected open fun onDataFetched(data: List<D>) = data
}
