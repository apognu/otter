package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_tracks.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TracksFragment : FunkwhaleFragment<Track, TracksAdapter>() {
  override val viewRes = R.layout.fragment_tracks
  override val recycler: RecyclerView get() = tracks

  lateinit var favoritesRepository: FavoritesRepository

  var albumId = 0
  var albumArtist = ""
  var albumTitle = ""
  var albumCover = ""

  companion object {
    fun new(album: Album): TracksFragment {
      return TracksFragment().apply {
        arguments = bundleOf(
          "albumId" to album.id,
          "albumArtist" to album.artist.name,
          "albumTitle" to album.title,
          "albumCover" to album.cover.original
        )
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    arguments?.apply {
      albumId = getInt("albumId")
      albumArtist = getString("albumArtist") ?: ""
      albumTitle = getString("albumTitle") ?: ""
      albumCover = getString("albumCover") ?: ""
    }

    adapter = TracksAdapter(context, FavoriteListener())
    repository = TracksRepository(context, albumId)
    favoritesRepository = FavoritesRepository(context)

    watchEventBus()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    Picasso.get()
      .load(albumCover)
      .noFade()
      .fit()
      .centerCrop()
      .into(cover)

    artist.text = albumArtist
    title.text = albumTitle
  }

  override fun onResume() {
    super.onResume()

    GlobalScope.launch(Main) {
      RequestBus.send(Request.GetCurrentTrack).wait<Response.CurrentTrack>()?.let { response ->
        adapter.currentTrack = response.track
        adapter.notifyDataSetChanged()
      }
    }

    play.setOnClickListener {
      CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))

      context.toast("All tracks were added to your queue")
    }

    queue.setOnClickListener {
      CommandBus.send(Command.AddToQueue(adapter.data))

      context.toast("All tracks were added to your queue")
    }
  }

  private fun watchEventBus() {
    GlobalScope.launch(Main) {
      for (message in EventBus.asChannel<Event>()) {
        when (message) {
          is Event.TrackPlayed -> {
            GlobalScope.launch(Main) {
              RequestBus.send(Request.GetCurrentTrack).wait<Response.CurrentTrack>()?.let { response ->
                adapter.currentTrack = response.track
                adapter.notifyDataSetChanged()
              }
            }
          }
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