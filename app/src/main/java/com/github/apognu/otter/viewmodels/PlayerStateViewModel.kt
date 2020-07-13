package com.github.apognu.otter.viewmodels

import androidx.lifecycle.*
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Track

class PlayerStateViewModel private constructor() : ViewModel() {
  companion object {
    private lateinit var instance: PlayerStateViewModel

    fun get(): PlayerStateViewModel {
      instance = if (::instance.isInitialized) instance else PlayerStateViewModel()

      return instance
    }
  }

  val isPlaying: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
  val isBuffering: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }
  val position: MutableLiveData<Triple<Int, Int, Int>> by lazy { MutableLiveData<Triple<Int, Int, Int>>() }

  val _track: MutableLiveData<Track> by lazy { MutableLiveData<Track>() }

  val track: LiveData<Track> by lazy {
    Transformations.switchMap(_track) {
      if (it == null) {
        return@switchMap null
      }

      Otter.get().database.tracks().getDecorated(it.id).map { Track.fromDecoratedEntity(it) }
    }
  }
}