package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.viewmodels.QueueViewModel
import kotlinx.android.synthetic.main.partial_queue.*
import kotlinx.android.synthetic.main.partial_queue.view.*

class LandscapeQueueFragment : Fragment() {
  private var adapter: TracksAdapter? = null

  private val viewModel = QueueViewModel.get()
  lateinit var favoritesRepository: FavoritesRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    favoritesRepository = FavoritesRepository(context)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    viewModel.queue.observe(viewLifecycleOwner) {
      refresh(it)
    }

    return inflater.inflate(R.layout.partial_queue, container, false).apply {
      adapter = TracksAdapter(context, FavoriteListener(), fromQueue = true).also {
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = it
      }
    }
  }

  override fun onResume() {
    super.onResume()

    queue?.visibility = View.GONE
    placeholder?.visibility = View.VISIBLE
  }

  private fun refresh(tracks: List<Track>) {
    adapter?.let {
      it.data = tracks.toMutableList()
      it.notifyDataSetChanged()

      if (it.data.isEmpty()) {
        queue?.visibility = View.GONE
        placeholder?.visibility = View.VISIBLE
      } else {
        queue?.visibility = View.VISIBLE
        placeholder?.visibility = View.GONE
      }
    }
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