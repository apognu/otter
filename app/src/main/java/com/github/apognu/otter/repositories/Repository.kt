package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.CacheItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import java.io.BufferedReader

interface Upstream<D> {
  fun fetch(size: Int = 0): Flow<Repository.Response<D>>
}

abstract class Repository<D : Any, C : CacheItem<D>> {
  protected val scope: CoroutineScope = CoroutineScope(Job() + IO)

  enum class Origin(val origin: Int) {
    Cache(0b01),
    Network(0b10)
  }

  data class Response<D>(val origin: Origin, val data: List<D>, val hasMore: Boolean)

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
          emit(Response(Origin.Cache, cache.data, false))
        }
      }
    }
  }.flowOn(IO)

  private fun fromNetwork(size: Int) = flow {
    upstream
      .fetch(size)
      .map { response -> Response(Origin.Network, onDataFetched(response.data), response.hasMore) }
      .collect { response -> emit(Response(Origin.Network, response.data, response.hasMore)) }
  }

  protected open fun onDataFetched(data: List<D>) = data
}
