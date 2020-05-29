package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.repositories.SearchRepository
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.activity_search.*
import java.net.URLEncoder
import java.util.*

class SearchActivity : AppCompatActivity() {
  private lateinit var adapter: TracksAdapter

  lateinit var repository: SearchRepository
  lateinit var favoritesRepository: FavoritesRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_search)

    adapter = TracksAdapter(this, FavoriteListener()).also {
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

          repository = SearchRepository(this@SearchActivity, query.toLowerCase(Locale.ROOT))
          favoritesRepository = FavoritesRepository(this@SearchActivity)

          search_spinner.visibility = View.VISIBLE
          search_no_results.visibility = View.GONE

          adapter.data.clear()
          adapter.notifyDataSetChanged()

          repository.fetch(Repository.Origin.Network.origin).untilNetwork { tracks, _, _ ->
            search_spinner.visibility = View.GONE
            search_empty.visibility = View.GONE

            when (tracks.isEmpty()) {
              true -> search_no_results.visibility = View.VISIBLE
              false -> adapter.data.addAll(tracks)
            }

            adapter.notifyItemRangeInserted(adapter.data.size, tracks.size)
          }
        }

        return true
      }

      override fun onQueryTextChange(newText: String?) = true
    })
  }

  inner class FavoriteListener : TracksAdapter.OnFavoriteListener {
    override fun onToggleFavorite(id: Int, state: Boolean) {
      when (state) {
        true -> favoritesRepository.addFavorite(id)
        false -> favoritesRepository.deleteFavorite(id)
      }
    }
  }
}