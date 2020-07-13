package com.github.apognu.otter.repositories

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map

interface Upstream<D> {
  fun fetch(size: Int = 0): Flow<Repository.Response<D>>
}

abstract class Repository<D : Any> {
  protected val scope: CoroutineScope = CoroutineScope(Job() + IO)

  data class Response<D>(val data: List<D>, val page: Int, val hasMore: Boolean)

  abstract val context: Context?
  abstract val upstream: Upstream<D>

  fun fetch(size: Int = 0) = channelFlow {
    upstream
      .fetch(size)
      .map { response -> Response(onDataFetched(response.data), response.page, response.hasMore) }
      .collect { response -> send(Response(response.data, response.page, response.hasMore)) }
  }

  protected open fun onDataFetched(data: List<D>) = data
}
