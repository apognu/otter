package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.asLiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.couchbase.lite.Database
import com.couchbase.lite.Expression
import com.couchbase.lite.Meta
import com.github.apognu.otter.models.Mediator
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.viewmodels.CouchbasePagingSource
import com.molo17.couchbase.lite.*
import kotlinx.coroutines.GlobalScope

class ArtistsRepository(override val context: Context, private val database: Database) : Repository<FunkwhaleArtist>() {
  private val mediator = Mediator(context, database, this)

  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/artists/?playable=true&ordering=name", FunkwhaleArtist.serializer())

  val pager = Pager(
    // config = PagingConfig(pageSize = AppContext.PAGE_SIZE, initialLoadSize = AppContext.PAGE_SIZE * 5, prefetchDistance = 10 * AppContext.PAGE_SIZE, maxSize = 25 * AppContext.PAGE_SIZE, enablePlaceholders = false),
    config = PagingConfig(pageSize = AppContext.PAGE_SIZE, initialLoadSize = 10, prefetchDistance = 2),
    pagingSourceFactory = {
      CouchbasePagingSource(this).apply {
        mediator.addListener {
          invalidate()
        }
      }
    },
    remoteMediator = mediator
  )

  fun insert(artists: List<FunkwhaleArtist>) = FunkwhaleArtist.persist(database, artists)

  fun all(page: Int) =
    select(all())
      .from(database)
      .where { "type" equalTo "artist" }
      .orderBy { "order".ascending() }
      .limit(AppContext.PAGE_SIZE, AppContext.PAGE_SIZE * page)
      .execute()

  fun get(id: Int) =
    select(all())
      .from(database)
      .where {
        ("type" equalTo "artist") and ("_id" equalTo "artist:$id")
      }
      .limit(1)
      .execute()
      .next()
      .run { Artist.from(this) }

  fun find(ids: List<Int>) =
    select(all())
      .from(database)
      .where { ("type" equalTo "artist") and (Meta.id.`in`(*ids.map { Expression.string("artist:$it") }.toTypedArray())) }
      .asFlow()
      .asLiveData(GlobalScope.coroutineContext)
}