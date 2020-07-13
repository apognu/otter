package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.SearchAdapter
import com.github.apognu.otter.fragments.AlbumsFragment
import com.github.apognu.otter.fragments.ArtistsFragment
import com.github.apognu.otter.models.dao.toDao
import com.github.apognu.otter.repositories.*
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.models.domain.Track
import com.google.android.exoplayer2.offline.Download
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.*

class SearchActivity : AppCompatActivity() {
  private lateinit var adapter: SearchAdapter

  lateinit var artistsRepository: ArtistsSearchRepository
  lateinit var albumsRepository: AlbumsSearchRepository
  lateinit var tracksRepository: TracksSearchRepository

  lateinit var favoritesRepository: FavoritesRepository

  var done = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_search)

    adapter = SearchAdapter(this, SearchResultClickListener(), FavoriteListener()).also {
      results.layoutManager = LinearLayoutManager(this)
      results.adapter = it
    }

    search.requestFocus()
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch(IO) {
      EventBus.get().collect { message ->
        when (message) {
          is Event.DownloadChanged -> refreshDownloadedTrack(message.download)
        }
      }
    }

    artistsRepository = ArtistsSearchRepository(this@SearchActivity, "")
    albumsRepository = AlbumsSearchRepository(this@SearchActivity, "")
    tracksRepository = TracksSearchRepository(this@SearchActivity, "")
    favoritesRepository = FavoritesRepository(this@SearchActivity)

    search.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(rawQuery: String?): Boolean {
        search.clearFocus()

        rawQuery?.let {
          done = 0

          val query = URLEncoder.encode(it, "UTF-8")

          artistsRepository.query = query.toLowerCase(Locale.ROOT)
          albumsRepository.query = query.toLowerCase(Locale.ROOT)
          tracksRepository.query = query.toLowerCase(Locale.ROOT)

          search_spinner.visibility = View.VISIBLE
          search_empty.visibility = View.GONE
          search_no_results.visibility = View.GONE

          adapter.artists.clear()
          adapter.albums.clear()
          adapter.tracks.clear()
          adapter.notifyDataSetChanged()

          artistsRepository.fetch().untilNetwork(lifecycleScope, IO) { artists, _, _ ->
            done++

            artists.forEach {
              Otter.get().database.artists().run {
                insert(it.toDao())

                adapter.artists.add(Artist.fromDecoratedEntity(getDecoratedBlocking(it.id)))
              }
            }

            lifecycleScope.launch(Main) {
              refresh()
            }
          }

          albumsRepository.fetch().untilNetwork(lifecycleScope, IO) { albums, _, _ ->
            done++

            albums.forEach {
              Otter.get().database.albums().run {
                insert(it.toDao())

                adapter.albums.add(Album.fromDecoratedEntity(getDecoratedBlocking(it.id)))
              }
            }

            lifecycleScope.launch(Main) {
              refresh()
            }
          }

          tracksRepository.fetch().untilNetwork(lifecycleScope, IO) { tracks, _, _ ->
            done++

            tracks.forEach {
              Otter.get().database.tracks().run {
                insertWithAssocs(it)

                adapter.tracks.add(Track.fromDecoratedEntity(getDecoratedBlocking(it.id)))
              }
            }

            lifecycleScope.launch(Main) {
              refresh()
            }
          }
        }

        return true
      }

      override fun onQueryTextChange(newText: String?) = true
    })
  }

  private fun refresh() {
    adapter.notifyDataSetChanged()

    if (adapter.artists.size + adapter.albums.size + adapter.tracks.size == 0) {
      search_no_results.visibility = View.VISIBLE
    } else {
      search_no_results.visibility = View.GONE
    }

    if (done == 3) {
      search_spinner.visibility = View.INVISIBLE
    }
  }

  private suspend fun refreshDownloadedTrack(download: Download) {
    if (download.state == Download.STATE_COMPLETED) {
      /* download.getMetadata()?.let { info ->
        adapter.tracks.withIndex().associate { it.value to it.index }.filter { it.key.id == info.id }.toList().getOrNull(0)?.let { match ->
          withContext(Dispatchers.Main) {
            adapter.tracks[match.second].downloaded = true
            adapter.notifyItemChanged(adapter.getPositionOf(SearchAdapter.ResultType.Track, match.second))
          }
        }
      } */
    }
  }

  inner class SearchResultClickListener : SearchAdapter.OnSearchResultClickListener {
    override fun onArtistClick(holder: View?, artist: Artist) {
      ArtistsFragment.openAlbums(this@SearchActivity, artist)
    }

    override fun onAlbumClick(holder: View?, album: Album) {
      AlbumsFragment.openTracks(this@SearchActivity, album)
    }
  }

  inner class FavoriteListener : SearchAdapter.OnFavoriteListener {
    override fun onToggleFavorite(id: Int, state: Boolean) {
      when (state) {
        true -> favoritesRepository.addFavorite(id)
        false -> favoritesRepository.deleteFavorite(id)
      }
    }
  }
}