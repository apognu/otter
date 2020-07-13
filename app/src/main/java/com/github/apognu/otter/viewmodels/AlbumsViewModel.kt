package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.models.domain.Upload

class AlbumsViewModel(private val artistId: Int? = null) : ViewModel() {
  val albums: LiveData<List<Album>> by lazy {
    if (artistId == null) {
      Transformations.map(Otter.get().database.albums().allDecorated()) {
        it.map { album -> Album.fromDecoratedEntity(album) }
      }
    } else {
      Transformations.map(Otter.get().database.albums().forArtistDecorated(artistId)) {
        it.map { album -> Album.fromDecoratedEntity(album) }
      }
    }
  }

  suspend fun tracks(): List<Track> {
    artistId?.let {
      val tracks = Otter.get().database.tracks().ofArtistBlocking(artistId)
      val uploads = Otter.get().database.uploads().findAllBlocking(tracks.map { it.id })

      return tracks.map {
        Track.fromDecoratedEntity(it).apply {
          this.uploads = uploads.filter { it.track_id == id }.map { Upload.fromEntity(it) }
        }
      }
    }

    return listOf()
  }
}

class AlbumViewModel(private val id: Int) : ViewModel() {
  val album: LiveData<Album> by lazy {
    Transformations.map(Otter.get().database.albums().getDecorated(id)) { album ->
      Album.fromDecoratedEntity(album)
    }
  }
}