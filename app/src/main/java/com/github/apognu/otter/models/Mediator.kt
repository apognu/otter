package com.github.apognu.otter.models

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.couchbase.lite.Database
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.log
import com.molo17.couchbase.lite.doInBatch
import com.molo17.couchbase.lite.from
import com.molo17.couchbase.lite.select
import com.molo17.couchbase.lite.where
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent


@OptIn(ExperimentalPagingApi::class)
class Mediator(private val context: Context, private val database: Database, private val repository: ArtistsRepository) : RemoteMediator<Int, Artist>(), KoinComponent {
  override suspend fun load(loadType: LoadType, state: PagingState<Int, Artist>): MediatorResult {
    loadType.log()

    return try {
      val key = when (loadType) {
        LoadType.REFRESH -> 1
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

        LoadType.APPEND -> {
          Cache.get(context, "key")?.readLine()?.toInt() ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
      }

      val response = withContext(IO) {
        repository.fetch((key - 1) * AppContext.PAGE_SIZE).take(1).first()
      }

      database.doInBatch {
        if (loadType == LoadType.REFRESH) {
          Cache.delete(context, "key")

          select("_id")
            .from(database)
            .where { "type" equalTo "artist" }
            .execute()
            .forEach { delete(getDocument(it.getString(0))) }
        }

        Cache.set(context, "key", (key + 1).toString().toByteArray())

        FunkwhaleArtist.persist(database, response.data, (key + 1) * 100)

        listeners.forEach {
          it()
          listeners.remove(it)
        }
      }

      return MediatorResult.Success(endOfPaginationReached = !response.hasMore)
    } catch (e: Exception) {
      MediatorResult.Error(e)
    }
  }

  private var listeners: MutableList<() -> Unit> = mutableListOf()

  fun addListener(listener: () -> Unit) {
    listeners.add(listener)
  }
}