package com.github.apognu.otter.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.CommandBus
import com.github.apognu.otter.viewmodels.QueueViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.android.synthetic.main.fragment_queue.view.*
import kotlinx.android.synthetic.main.partial_queue.*
import kotlinx.android.synthetic.main.partial_queue.view.*

class QueueFragment : BottomSheetDialogFragment() {
  private var adapter: TracksAdapter? = null

  private val viewModel = QueueViewModel.get()
  lateinit var favoritesRepository: FavoritesRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    favoritesRepository = FavoritesRepository(context)

    setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FloatingBottomSheet)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState).apply {
      setOnShowListener {
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
          BottomSheetBehavior.from(it).skipCollapsed = true
        }
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    viewModel.queue.observe(viewLifecycleOwner) {
      refresh(it)
    }

    return inflater.inflate(R.layout.fragment_queue, container, false).apply {
      adapter = TracksAdapter(context, FavoriteListener(), fromQueue = true).also {
        included.queue.layoutManager = LinearLayoutManager(context)
        included.queue.adapter = it
      }
    }
  }

  override fun onResume() {
    super.onResume()

    included.queue?.visibility = View.GONE
    placeholder?.visibility = View.VISIBLE

    queue_shuffle.setOnClickListener {
      CommandBus.send(Command.ShuffleQueue)
    }

    queue_clear.setOnClickListener {
      CommandBus.send(Command.ClearQueue)
    }
  }

  private fun refresh(tracks: List<Track>) {
    included?.let { included ->
      adapter?.let {
        it.data = tracks.toMutableList()
        it.notifyDataSetChanged()

        if (it.data.isEmpty()) {
          included.queue?.visibility = View.GONE
          placeholder?.visibility = View.VISIBLE
        } else {
          included.queue?.visibility = View.VISIBLE
          placeholder?.visibility = View.GONE
        }
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