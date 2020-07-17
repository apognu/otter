package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.TracksRepository
import kotlinx.coroutines.delay

class TracksViewModel(private val repository: TracksRepository, playerViewModel: PlayerStateViewModel, private val albumId: Int) : ViewModel() {
  private val _downloaded = liveData {
    while (true) {
      emit(repository.downloaded())
      delay(5000)
    }
  }

  private val _current = playerViewModel.track

  private val _tracks: LiveData<List<Track>> by lazy {
    Transformations.map(repository.ofAlbums(listOf(albumId))) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }

  private val _favorites: LiveData<List<Track>> by lazy {
    Transformations.map(repository.favorites()) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }

  val tracks = MediatorLiveData<List<Track>>().apply {
    addSource(_tracks) { mergeTracks(_tracks, _current, _downloaded) }
    addSource(_current) { mergeTracks(_tracks, _current, _downloaded) }
    addSource(_downloaded) { mergeTracks(_tracks, _current, _downloaded) }
  }

  val favorites = MediatorLiveData<List<Track>>().apply {
    addSource(_favorites) { mergeFavorites(_favorites, _current, _downloaded) }
    addSource(_current) { mergeFavorites(_favorites, _current, _downloaded) }
    addSource(_downloaded) { mergeFavorites(_favorites, _current, _downloaded) }
  }

  private fun mergeTracks(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _downloaded: LiveData<List<Int>>) {
    tracks.value = merge(_tracks, _current, _downloaded) ?: return
  }

  private fun mergeFavorites(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _downloaded: LiveData<List<Int>>) {
    favorites.value = merge(_tracks, _current, _downloaded) ?: return
  }

  private fun merge(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _downloaded: LiveData<List<Int>>): List<Track>? {
    val _tracks = _tracks.value
    val _current = _current.value
    val _downloaded = _downloaded.value

    if (_tracks == null || _downloaded == null) {
      return null
    }

    return _tracks.map { track ->
      track.current = _current?.id == track.id
      track.cached = repository.isCached(track)
      track.downloaded = _downloaded.contains(track.id)
      track
    }
  }
}
