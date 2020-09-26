package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.AlbumsRepository
import com.github.apognu.otter.repositories.TracksRepository

class AlbumsViewModel(private val repository: AlbumsRepository, private val tracksRepository: TracksRepository, private val artistId: Int? = null) : ViewModel() {
  val albums: LiveData<List<Album>> by lazy {
    if (artistId == null) {
      Transformations.map(repository.all()) {
        it.map { result -> Album.from(result) }
      }
    } else {
      Transformations.map(repository.ofArtist(artistId)) {
        it.map { result -> Album.from(result) }
      }
    }
  }

  suspend fun tracks(): List<Track> {
    artistId?.let {
      val tracks = tracksRepository.ofArtistBlocking(artistId)

      return tracks.map {
        Track.fromDecoratedEntity(it)
      }
    }

    return listOf()
  }
}
