package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.dao.PlaylistEntity
import com.github.apognu.otter.models.domain.Track

class PlaylistsViewModel : ViewModel() {
  val playlists: LiveData<List<PlaylistEntity>> by lazy { Otter.get().database.playlists().all() }
}

class PlaylistViewModel(playlistId: Int) : ViewModel() {
  val tracks: LiveData<List<Track>> by lazy {
    Transformations.map(Otter.get().database.playlists().tracksFor(playlistId)) {
      it.map { track -> Track.fromDecoratedEntity(track) }
    }
  }
}
