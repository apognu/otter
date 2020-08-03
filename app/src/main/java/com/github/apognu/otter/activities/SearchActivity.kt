package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.SearchAdapter
import com.github.apognu.otter.fragments.AlbumsFragment
import com.github.apognu.otter.fragments.ArtistsFragment
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.viewmodels.SearchViewModel
import kotlinx.android.synthetic.main.activity_search.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.net.URLEncoder

class SearchActivity : AppCompatActivity() {
  private val viewModel by viewModel<SearchViewModel>()
  private val favoritesRepository by inject<FavoritesRepository>()

  private lateinit var adapter: SearchAdapter

  var done = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_search)

    adapter = SearchAdapter(this, SearchResultClickListener(), FavoriteListener()).also {
      results.layoutManager = LinearLayoutManager(this)
      results.adapter = it
    }

    search.requestFocus()

    viewModel.artists.observe(this) { artists ->
      if (adapter.artists.size != artists.size) done++

      adapter.artists = artists.toMutableSet()

      lifecycleScope.launch(Main) {
        refresh()
      }
    }

    viewModel.albums.observe(this) { albums ->
      if (adapter.albums.size != albums.size) done++

      adapter.albums = albums.toMutableSet()

      lifecycleScope.launch(Main) {
        refresh()
      }
    }

    viewModel.tracks.observe(this) { tracks ->
      if (adapter.tracks.size != tracks.size) done++

      adapter.tracks = tracks.toMutableSet()

      lifecycleScope.launch(Main) {
        refresh()
      }
    }
  }

  override fun onResume() {
    super.onResume()

    search.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(rawQuery: String?): Boolean {
        search.clearFocus()

        rawQuery?.let {
          done = 0

          adapter.artists.clear()
          adapter.albums.clear()
          adapter.tracks.clear()
          adapter.notifyDataSetChanged()

          val query = URLEncoder.encode(it, "UTF-8")

          viewModel.search(query)

          search_spinner.visibility = View.VISIBLE
          search_empty.visibility = View.GONE
          search_no_results.visibility = View.GONE
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