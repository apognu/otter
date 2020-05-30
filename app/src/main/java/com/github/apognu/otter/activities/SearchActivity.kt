package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
      override fun onQueryTextSubmit(query: String?): Boolean {
        search.clearFocus()

        query?.let {
          val query = URLEncoder.encode(it, "UTF-8")

          tracksRepository = TracksSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          albumsRepository = AlbumsSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          artistsRepository = ArtistsSearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          favoritesRepository = FavoritesRepository(this@SearchActivity)

          search_spinner.visibility = View.VISIBLE
          search_no_results.visibility = View.GONE

          adapter.artists.clear()
          adapter.albums.clear()
          adapter.tracks.clear()
          adapter.notifyDataSetChanged()

          artistsRepository.fetch(Repository.Origin.Network.origin).untilNetwork { artists, _, _ ->
            when (artists.isEmpty()) {
              true -> search_no_results.visibility = View.VISIBLE
              false -> adapter.artists.addAll(artists)
            }

            adapter.notifyDataSetChanged()
          }

          albumsRepository.fetch(Repository.Origin.Network.origin).untilNetwork { albums, _, _ ->
            when (albums.isEmpty()) {
              true -> search_no_results.visibility = View.VISIBLE
              false -> adapter.albums.addAll(albums)
            }

            adapter.notifyDataSetChanged()
          }

          tracksRepository.fetch(Repository.Origin.Network.origin).untilNetwork { tracks, _, _ ->
            search_spinner.visibility = View.GONE
            search_empty.visibility = View.GONE

            when (tracks.isEmpty()) {
              true -> search_no_results.visibility = View.VISIBLE
              false -> adapter.tracks.addAll(tracks)
            }

            adapter.notifyDataSetChanged()
          }
        }

        return true
      }

      override fun onQueryTextChange(newText: String?) = true
    })
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