package com.github.apognu.otter.models

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.github.apognu.otter.models.dao.DecoratedArtistEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.toDao
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.log
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import org.koin.core.KoinComponent


@OptIn(ExperimentalPagingApi::class)
class Mediator(private val context: Context, private val database: OtterDatabase, private val repository: ArtistsRepository) : RemoteMediator<Int, DecoratedArtistEntity>(), KoinComponent {
  override suspend fun load(loadType: LoadType, state: PagingState<Int, DecoratedArtistEntity>): MediatorResult {
    return try {
      val key = when (loadType) {
        LoadType.REFRESH -> 1
        LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

        LoadType.APPEND -> {
          Cache.get(context, "key")?.readLine()?.toInt() ?: return MediatorResult.Success(endOfPaginationReached = true)
        }
      }

      key.log("fetching page")

      val response = repository.fetch((key - 1) * AppContext.PAGE_SIZE).take(1).first()

      database.withTransaction {
        if (loadType == LoadType.REFRESH) {
          Cache.delete(context, "key")
          database.artists().deleteAll()
        }

        Cache.set(context, "key", (key + 1).toString().toByteArray())

        response.data.forEach {
          database.artists().insert(it.toDao())
        }
      }

      return MediatorResult.Success(endOfPaginationReached = !response.hasMore)
    } catch (e: Exception) {
      MediatorResult.Error(e)
    }
  }
}