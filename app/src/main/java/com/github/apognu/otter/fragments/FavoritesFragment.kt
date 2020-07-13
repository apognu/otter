package com.github.apognu.otter.fragments

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.FavoritesAdapter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.CommandBus
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.viewmodels.PlayerStateViewModel
import com.github.apognu.otter.viewmodels.TracksViewModel
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FavoritesFragment : LiveOtterFragment<FunkwhaleTrack, Track, FavoritesAdapter>() {
  override val liveData = TracksViewModel(0).favorites
  override val viewRes = R.layout.fragment_favorites
  override val recycler: RecyclerView get() = favorites
  override val alwaysRefresh = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = FavoritesAdapter(context, FavoriteListener())
    repository = FavoritesRepository(context)

    PlayerStateViewModel.get().track.observe(this) { refreshCurrentTrack(it) }

    watchEventBus()
  }

  override fun onResume() {
    super.onResume()

    play.setOnClickListener {
      CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))
    }
  }

  private fun watchEventBus() {
    lifecycleScope.launch(Main) {
      EventBus.get().collect { message ->
        when (message) {
          // is Event.DownloadChanged -> refreshDownloadedTrack(message.download)
        }
      }
    }
  }

  private suspend fun refreshDownloadedTracks() {
    val downloaded = TracksRepository.getDownloadedIds() ?: listOf()

    /* withContext(Main) {
      adapter.data = adapter.data.map {
        it.downloaded = downloaded.contains(it.id)
        it
      }.toMutableList()

      adapter.notifyDataSetChanged()
    } */
  }

  /* private suspend fun refreshDownloadedTrack(download: Download) {
    if (download.state == Download.STATE_COMPLETED) {
      download.getMetadata()?.let { info ->
        adapter.data.withIndex().associate { it.value to it.index }.filter { it.key.id == info.id }.toList().getOrNull(0)?.let { match ->
          withContext(Main) {
            adapter.data[match.second].downloaded = true
            adapter.notifyItemChanged(match.second)
          }
        }
      }
    }
  } */

  private fun refreshCurrentTrack(track: Track?) {
    track?.let {
      adapter.currentTrack = track
      adapter.notifyDataSetChanged()
    }
  }

  inner class FavoriteListener : FavoritesAdapter.OnFavoriteListener {
    override fun onToggleFavorite(id: Int, state: Boolean) {
      (repository as? FavoritesRepository)?.let { repository ->
        when (state) {
          true -> repository.addFavorite(id)
          false -> repository.deleteFavorite(id)
        }
      }
    }
  }
}
