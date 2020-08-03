package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.github.apognu.otter.models.Mediator
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext
import kotlinx.coroutines.flow.map

class ArtistsViewModel(repository: ArtistsRepository, mediator: Mediator) : ViewModel() {
  private val pager = Pager(
    config = PagingConfig(pageSize = AppContext.PAGE_SIZE, initialLoadSize = AppContext.PAGE_SIZE * 5, prefetchDistance = 10 * AppContext.PAGE_SIZE, maxSize = 25 * AppContext.PAGE_SIZE, enablePlaceholders = false),
    pagingSourceFactory = repository.allPaged().asPagingSourceFactory(),
    remoteMediator = mediator
  )

  val artistsPaged = pager
    .flow
    .map { artists -> artists.map { Artist.fromDecoratedEntity(it) } }
    .cachedIn(viewModelScope)
    .asLiveData()

  val artists: LiveData<List<Artist>> = repository.all().map { artists ->
    artists.map { Artist.fromDecoratedEntity(it) }
  }
}
