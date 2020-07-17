package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.models.dao.PlaylistEntity
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.PlaylistTracksRepository
import com.github.apognu.otter.repositories.PlaylistsRepository
import com.github.apognu.otter.repositories.TracksRepository
import kotlinx.coroutines.delay

class PlaylistsViewModel(private val repository: PlaylistsRepository) : ViewModel() {
  val playlists: LiveData<List<PlaylistEntity>> by lazy { repository.all() }
}

class PlaylistViewModel(private val repository: PlaylistTracksRepository, private val tracksRepository: TracksRepository, playerViewModel: PlayerStateViewModel, playlistId: Int) : ViewModel() {
  private val _downloaded = liveData {
    while (true) {
      emit(tracksRepository.downloaded())
      delay(5000)
    }
  }

  private val _current = playerViewModel.track

  private val _tracks: LiveData<List<Track>> by lazy {
    Transformations.map(repository.tracks(playlistId)) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }

  val tracks = MediatorLiveData<List<Track>>().apply {
    addSource(_tracks) { merge(_tracks, _current, _downloaded) }
    addSource(_current) { merge(_tracks, _current, _downloaded) }
    addSource(_downloaded) { merge(_tracks, _current, _downloaded) }
  }

  private fun merge(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _downloaded: LiveData<List<Int>>) {
    val _tracks = _tracks.value
    val _current = _current.value
    val _downloaded = _downloaded.value

    if (_tracks == null || _downloaded == null) {
      return
    }

    tracks.value = _tracks.map { track ->
      track.current = _current?.id == track.id
      track.cached = tracksRepository.isCached(track)
      track.downloaded = _downloaded.contains(track.id)
      track
    }
  }
}
