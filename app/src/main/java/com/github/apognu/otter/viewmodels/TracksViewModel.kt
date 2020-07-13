package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.utils.maybeNormalizeUrl
import kotlinx.coroutines.delay

class TracksViewModel(private val albumId: Int) : ViewModel() {
  private val _cached = liveData {
    while (true) {
      emit(Otter.get().exoCache.keys)
      delay(5000)
    }
  }

  private val _current = PlayerStateViewModel.get().track

  private val _tracks: LiveData<List<Track>> by lazy {
    Transformations.map(Otter.get().database.tracks().ofAlbumsDecorated(listOf(albumId))) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }

  private val _favorites: LiveData<List<Track>> by lazy {
    Transformations.map(Otter.get().database.tracks().favorites()) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }

  val tracks = MediatorLiveData<List<Track>>().apply {
    addSource(_tracks) { mergeTracks(_tracks, _current, _cached) }
    addSource(_current) { mergeTracks(_tracks, _current, _cached) }
    addSource(_cached) { mergeTracks(_tracks, _current, _cached) }
  }

  val favorites = MediatorLiveData<List<Track>>().apply {
    addSource(_favorites) { mergeFavorites(_favorites, _current, _cached) }
    addSource(_current) { mergeFavorites(_favorites, _current, _cached) }
    addSource(_cached) { mergeFavorites(_favorites, _current, _cached) }
  }

  private fun mergeTracks(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _cached: LiveData<Set<String>>) {
    tracks.value = merge(_tracks, _current, _cached) ?: return
  }

  private fun mergeFavorites(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _cached: LiveData<Set<String>>) {
    favorites.value = merge(_tracks, _current, _cached) ?: return
  }

  private fun merge(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _cached: LiveData<Set<String>>): List<Track>? {
    val _tracks = _tracks.value
    val _current = _current.value
    val _cached = _cached.value

    if (_tracks == null || _cached == null) {
      return null
    }

    return _tracks.map { track ->
      track.current = _current?.id == track.id
      track.cached = _cached.contains(maybeNormalizeUrl(track.bestUpload()?.listen_url))
      track
    }
  }
}
