package com.github.apognu.otter.viewmodels

import androidx.lifecycle.ViewModel
import com.github.apognu.otter.repositories.AlbumsSearchRepository
import com.github.apognu.otter.repositories.ArtistsSearchRepository
import com.github.apognu.otter.repositories.TracksSearchRepository

class SearchViewModel(private val artistsRepository: ArtistsSearchRepository, private val albumsRepository: AlbumsSearchRepository, private val tracksRepository: TracksSearchRepository) : ViewModel() {
  val artists = artistsRepository.results
  val albums = albumsRepository.results
  val tracks = tracksRepository.results

  fun search(term: String) {
    artistsRepository.search(term)
    albumsRepository.search(term)
    tracksRepository.search(term)
  }
}