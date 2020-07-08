package com.github.apognu.otter.fragments

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.FavoritesAdapter
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.offline.Download
import kotlinx.android.synthetic.main.fragment_favorites.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : FunkwhaleFragment<Track, FavoritesAdapter>() {
  override val viewRes = R.layout.fragment_favorites
  override val recycler: RecyclerView get() = favorites
  override val alwaysRefresh = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = FavoritesAdapter(context, FavoriteListener())
    repository = FavoritesRepository(context)

    watchEventBus()
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch(IO) {
      RequestBus.send(Request.GetCurrentTrack).wait<Response.CurrentTrack>()?.let { response ->
        withContext(Main) {
          adapter.currentTrack = response.track
          adapter.notifyDataSetChanged()
        }
      }

      refreshDownloadedTracks()
    }

    play.setOnClickListener {
      CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))
    }
  }

  private fun watchEventBus() {
    lifecycleScope.launch(Main) {
      EventBus.get().collect { message ->
        when (message) {
          is Event.DownloadChanged -> refreshDownloadedTrack(message.download)
        }
      }
    }

    lifecycleScope.launch(Main) {
      CommandBus.get().collect { command ->
        when (command) {
          is Command.RefreshTrack -> refreshCurrentTrack(command.track)
        }
      }
    }
  }

  private suspend fun refreshDownloadedTracks() {
    val downloaded = TracksRepository.getDownloadedIds() ?: listOf()

    withContext(Main) {
      adapter.data = adapter.data.map {
        it.downloaded = downloaded.contains(it.id)
        it
      }.toMutableList()

      adapter.notifyDataSetChanged()
    }
  }

  private suspend fun refreshDownloadedTrack(download: Download) {
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
  }

  private fun refreshCurrentTrack(track: Track?) {
    track?.let {
      adapter.currentTrack?.current = false
      adapter.currentTrack = track.apply {
        current = true
      }
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
