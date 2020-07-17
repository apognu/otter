package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import kotlinx.coroutines.delay

class FavoritesViewModel(private val repository: FavoritesRepository, private val tracksRepository: TracksRepository) : ViewModel() {
  private val _downloaded = liveData {
    while (true) {
      emit(tracksRepository.downloaded())
      delay(5000)
    }
  }

  private val _albums: LiveData<List<Album>> by lazy {
    Transformations.switchMap(_favorites) { tracks ->
      val ids = tracks.mapNotNull { it.album?.id }

      Transformations.map(repository.find(ids)) { albums ->
        albums.map { album -> Album.fromDecoratedEntity(album) }
      }
    }
  }

  private val _favorites: LiveData<List<Track>> by lazy {
    Transformations.switchMap(repository.all()) {
      val ids = it.map { favorite -> favorite.track_id }

      Transformations.map(tracksRepository.find(ids)) { tracks ->
        tracks.map { track -> Track.fromDecoratedEntity(track) }.sortedBy { it.title }
      }
    }
  }

  val favorites = MediatorLiveData<List<Track>>().apply {
    addSource(_favorites) { merge(_favorites, _albums, _downloaded) }
    addSource(_albums) { merge(_favorites, _albums, _downloaded) }
    addSource(_downloaded) { merge(_favorites, _albums, _downloaded) }
  }

  private fun merge(_tracks: LiveData<List<Track>>, _albums: LiveData<List<Album>>, _downloaded: LiveData<List<Int>>) {
    val _tracks = _tracks.value
    val _albums = _albums.value
    val _downloaded = _downloaded.value

    if (_tracks == null || _albums == null || _downloaded == null) {
      return
    }

    favorites.value = _tracks.map { track ->
      track.cached = tracksRepository.isCached(track)
      track.downloaded = _downloaded.contains(track.id)
      track
    }
  }
}