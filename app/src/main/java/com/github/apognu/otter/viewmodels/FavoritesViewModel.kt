package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.models.domain.Upload

class FavoritesViewModel : ViewModel() {
  companion object {
    private lateinit var instance: FavoritesViewModel

    fun get(): FavoritesViewModel {
      instance = if (::instance.isInitialized) instance else FavoritesViewModel()

      return instance
    }
  }

  private val _albums: LiveData<List<Album>> by lazy {
    Transformations.switchMap(_favorites) { tracks ->
      val ids = tracks.mapNotNull { it.album?.id }

      Transformations.map(Otter.get().database.albums().findAllDecorated(ids)) { albums ->
        albums.map { album -> Album.fromDecoratedEntity(album) }
      }
    }
  }

  private val _favorites: LiveData<List<Track>> by lazy {
    Transformations.switchMap(Otter.get().database.favorites().all()) {
      val ids = it.map { favorite -> favorite.track_id }

      Transformations.map(Otter.get().database.tracks().findAllDecorated(ids)) { tracks ->
        tracks.map { track -> Track.fromDecoratedEntity(track) }.sortedBy { it.title }
      }
    }
  }

  private val _uploads: LiveData<List<Upload>> by lazy {
    Transformations.switchMap(_favorites) { tracks ->
      val ids = tracks.mapNotNull { it.album?.id }

      Transformations.map(Otter.get().database.uploads().findAll(ids)) { uploads ->
        uploads.map { upload -> Upload.fromEntity(upload) }
      }
    }
  }

  val favorites = MediatorLiveData<List<Track>>().apply {
    addSource(_favorites) { merge(_favorites, _albums, _uploads) }
    addSource(_albums) { merge(_favorites, _albums, _uploads) }
    addSource(_uploads) { merge(_favorites, _albums, _uploads) }
  }

  private fun merge(_tracks: LiveData<List<Track>>, _albums: LiveData<List<Album>>, _uploads: LiveData<List<Upload>>) {
    val _tracks = _tracks.value
    val _albums = _albums.value
    val _uploads = _uploads.value

    if (_tracks == null || _albums == null || _uploads == null) {
      return
    }

    favorites.value = _tracks.map { track ->
      track.uploads = _uploads.filter { upload -> upload.track_id == track.id }
      track
    }
  }
}