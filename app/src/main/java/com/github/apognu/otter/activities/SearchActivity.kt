package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.SearchAdapter
import com.github.apognu.otter.fragments.AlbumsFragment
import com.github.apognu.otter.fragments.ArtistsFragment
import com.github.apognu.otter.repositories.*
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.Artist
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.activity_search.*
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
  }

  override fun onResume() {
    super.onResume()

    search.requestFocus()

    search.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(rawQuery: String?): Boolean {
        search.clearFocus()

        rawQuery?.let {
          done = 0

          val query = URLEncoder.encode(it, "UTF-8")

          tracksRepository = TracksSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          albumsRepository = AlbumsSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          artistsRepository = ArtistsSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          favoritesRepository = FavoritesRepository(this@SearchActivity)

          search_spinner.visibility = View.VISIBLE
          search_empty.visibility = View.GONE
          search_no_results.visibility = View.GONE

          adapter.artists.clear()
          adapter.albums.clear()
          adapter.tracks.clear()
          adapter.notifyDataSetChanged()

          artistsRepository.fetch(Repository.Origin.Network.origin).untilNetwork(lifecycleScope) { artists, _, _, _ ->
            done++

            adapter.artists.addAll(artists)
            refresh()
          }

          albumsRepository.fetch(Repository.Origin.Network.origin).untilNetwork(lifecycleScope) { albums, _, _ ,_ ->
            done++

            adapter.albums.addAll(albums)
            refresh()
          }

          tracksRepository.fetch(Repository.Origin.Network.origin).untilNetwork(lifecycleScope) { tracks, _, _, _ ->
            done++

            adapter.tracks.addAll(tracks)
            refresh()
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