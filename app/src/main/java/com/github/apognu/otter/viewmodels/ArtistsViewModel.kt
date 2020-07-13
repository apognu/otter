package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations.map
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Artist

class ArtistsViewModel : ViewModel() {
  companion object {
    private lateinit var instance: ArtistsViewModel

    fun get(): ArtistsViewModel {
      instance = if (::instance.isInitialized) instance else ArtistsViewModel()
      return instance
    }
  }

  val artists: LiveData<List<Artist>> = Otter.get().database.artists().allDecorated().map {
    it.map { Artist.fromDecoratedEntity(it) }
  }
}

class ArtistViewModel(private val id: Int) : ViewModel() {
  val artist: LiveData<Artist> by lazy {
    map(Otter.get().database.artists().getDecorated(id)) { artist ->
      Artist.fromDecoratedEntity(artist)
    }
  }
}