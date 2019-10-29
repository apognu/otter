package com.github.apognu.otter.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.repositories.SearchRepository
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*

class SearchActivity : AppCompatActivity() {
  private lateinit var adapter: TracksAdapter

  lateinit var repository: SearchRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_search)

    adapter = TracksAdapter(this).also {
      results.layoutManager = LinearLayoutManager(this)
      results.adapter = it
    }
  }

  override fun onResume() {
    super.onResume()

    search.requestFocus()

    search.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let {
          repository = SearchRepository(this@SearchActivity, it.toLowerCase(Locale.ROOT))

          search_spinner.visibility = View.VISIBLE
          search_no_results.visibility = View.GONE

          adapter.data.clear()
          adapter.notifyDataSetChanged()

          repository.fetch(Repository.Origin.Network.origin).untilNetwork { tracks, _ ->
            search_spinner.visibility = View.GONE
            search_empty.visibility = View.GONE

            when (tracks.isEmpty()) {
              true -> search_no_results.visibility = View.VISIBLE
              false -> adapter.data = tracks.toMutableList()
            }

            adapter.notifyDataSetChanged()
          }
        }

        return true
      }

      override fun onQueryTextChange(newText: String?) = true

    })
  }
}