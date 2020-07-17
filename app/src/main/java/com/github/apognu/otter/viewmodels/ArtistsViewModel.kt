package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository

class ArtistsViewModel(private val repository: ArtistsRepository) : ViewModel() {
  val artists: LiveData<List<Artist>> = repository.all().map { artists ->
    artists.map { Artist.fromDecoratedEntity(it) }
  }
}
