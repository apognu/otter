package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.map
import com.couchbase.lite.Database
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.models.domain.Track
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ArtistsSearchRepository(override val context: Context?, private val repository: ArtistsRepository, var query: String = "") : Repository<FunkwhaleArtist>() {
  override val upstream: Upstream<FunkwhaleArtist>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/artists/?playable=true&q=$query", FunkwhaleArtist.serializer())

  private val ids: MutableList<Int> = mutableListOf()

  private val _ids: MutableLiveData<List<Int>> = MutableLiveData()

  val results: LiveData<List<Artist>> = Transformations.switchMap(_ids) {
    repository.find(it).map { result ->
      result.map { artist -> Artist.from(artist) }
    }
  }

  override fun onDataFetched(data: List<FunkwhaleArtist>): List<FunkwhaleArtist> {
    repository.insert(data)

    ids.addAll(data.map { it.id })
    _ids.postValue(ids)

    return super.onDataFetched(data)
  }

  fun search(term: String) {
    ids.clear()
    _ids.postValue(listOf())
    query = term

    scope.launch(IO) {
      fetch().collect()
    }
  }
}

class AlbumsSearchRepository(override val context: Context?, private val repository: AlbumsRepository, var query: String = "") : Repository<FunkwhaleAlbum>() {
  override val upstream: Upstream<FunkwhaleAlbum>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/albums/?playable=true&q=$query", FunkwhaleAlbum.serializer())

  private val ids: MutableList<Int> = mutableListOf()
  private val _ids: MutableLiveData<List<Int>> = MutableLiveData()

  val results: LiveData<List<Album>> = Transformations.switchMap(_ids) {
    repository.find(it).map { result ->
      result.map { album -> Album.from(album) }
    }
  }

  override fun onDataFetched(data: List<FunkwhaleAlbum>): List<FunkwhaleAlbum> {
    repository.insert(data)

    ids.addAll(data.map { it.id })
    _ids.postValue(ids)

    return super.onDataFetched(data)
  }

  fun search(term: String) {
    ids.clear()
    _ids.postValue(listOf())

    query = term

    scope.launch(IO) {
      fetch().collect()
    }
  }
}

class TracksSearchRepository(override val context: Context?, private val repository: TracksRepository, var query: String = "") : Repository<FunkwhaleTrack>() {
  override val upstream: Upstream<FunkwhaleTrack>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&q=$query", FunkwhaleTrack.serializer())

  private val ids: MutableList<Int> = mutableListOf()
  private val _ids: MutableLiveData<List<Int>> = MutableLiveData()

  val results: LiveData<List<Track>> = Transformations.switchMap(_ids) {
    repository.find(it).map { result ->
      result.map { track -> Track.from(track) }
    }
  }

  override fun onDataFetched(data: List<FunkwhaleTrack>): List<FunkwhaleTrack> {
    repository.insert(data)

    ids.addAll(data.map { it.id })
    _ids.postValue(ids)

    return super.onDataFetched(data)
  }

  fun search(term: String) {
    ids.clear()
    _ids.postValue(listOf())
    query = term

    scope.launch(IO) {
      fetch().collect()
    }
  }
}