package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.QueueRepository
import com.github.apognu.otter.utils.maybeNormalizeUrl
import kotlinx.coroutines.delay

class QueueViewModel private constructor() : ViewModel() {
  companion object {
    private lateinit var instance: QueueViewModel

    fun get(): QueueViewModel {
      instance = if (::instance.isInitialized) instance else QueueViewModel()

      return instance
    }
  }

  private val queueRepository = QueueRepository(viewModelScope)

  private val _cached = liveData {
    while (true) {
      emit(Otter.get().exoCache.keys)
      delay(5000)
    }
  }

  private val _current = PlayerStateViewModel.get().track

  private val _queue: LiveData<List<Track>> by lazy {
    Transformations.map(queueRepository.all()) { tracks ->
      tracks.map { Track.fromDecoratedEntity(it) }
    }
  }

  val queue = MediatorLiveData<List<Track>>().apply {
    addSource(_queue) { merge(_queue, _current, _cached) }
    addSource(_current) { merge(_queue, _current, _cached) }
    addSource(_cached) { merge(_queue, _current, _cached) }
  }

  private fun merge(_tracks: LiveData<List<Track>>, _current: LiveData<Track>, _cached: LiveData<Set<String>>) {
    val _tracks = _tracks.value
    val _current = _current.value
    val _cached = _cached.value

    if (_tracks == null || _cached == null) {
      return
    }

    queue.value = _tracks.map { track ->
      track.current = _current?.id == track.id
      track.cached = _cached.contains(maybeNormalizeUrl(track.bestUpload()?.listen_url))
      track
    }
  }
}