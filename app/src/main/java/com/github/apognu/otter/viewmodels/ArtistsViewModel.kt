package com.github.apognu.otter.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext

class CouchbasePagingSource(val repository: ArtistsRepository) : PagingSource<Int, Artist>() {
  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Artist> {
    return try {
      val page = params.key ?: 0

      val artists = repository.all(page).map { Artist.from(it) }
      val prevKey = if (page > 0) page - 1 else null
      val nextKey = if (artists.isNotEmpty() && artists.size == AppContext.PAGE_SIZE) page + 1 else null

      LoadResult.Page(
        data = artists,
        prevKey = prevKey,
        nextKey = nextKey
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }
}

class ArtistsViewModel(repository: ArtistsRepository) : ViewModel() {
  val artists = repository.pager
    .flow
    .cachedIn(viewModelScope)
    .asLiveData(viewModelScope.coroutineContext)
}
