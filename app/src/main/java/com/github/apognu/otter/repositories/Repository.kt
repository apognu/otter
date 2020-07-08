package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.CacheItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import kotlin.math.ceil

interface Upstream<D> {
  fun fetch(size: Int = 0): Flow<Repository.Response<D>>
}

abstract class Repository<D : Any, C : CacheItem<D>> {
  protected val scope: CoroutineScope = CoroutineScope(Job() + IO)

  enum class Origin(val origin: Int) {
    Cache(0b01),
    Network(0b10)
  }

  data class Response<D>(val origin: Origin, val data: List<D>, val page: Int, val hasMore: Boolean)

  abstract val context: Context?
  abstract val cacheId: String?
  abstract val upstream: Upstream<D>

  open fun cache(data: List<D>): C? = null
  protected open fun uncache(reader: BufferedReader): C? = null

  fun fetch(upstreams: Int = Origin.Cache.origin and Origin.Network.origin, size: Int = 0): Flow<Response<D>> = flow {
    if (Origin.Cache.origin and upstreams == upstreams) fromCache().collect { emit(it) }
    if (Origin.Network.origin and upstreams == upstreams) fromNetwork(size).collect { emit(it) }
  }

  private fun fromCache() = flow {
    cacheId?.let { cacheId ->
      Cache.get(context, cacheId)?.let { reader ->
        uncache(reader)?.let { cache ->
          return@flow emit(Response(Origin.Cache, cache.data, ceil(cache.data.size / AppContext.PAGE_SIZE.toDouble()).toInt(), false))
        }
      }

      return@flow emit(Response(Origin.Cache, listOf(), 1, false))
    }
  }.flowOn(IO)

  private fun fromNetwork(size: Int) = flow {
    upstream
      .fetch(size)
      .map { response -> Response(Origin.Network, onDataFetched(response.data), response.page, response.hasMore) }
      .collect { response -> emit(Response(Origin.Network, response.data, response.page, response.hasMore)) }
  }

  protected open fun onDataFetched(data: List<D>) = data
}
