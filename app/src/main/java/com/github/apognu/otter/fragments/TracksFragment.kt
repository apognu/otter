package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.offline.Download
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_tracks.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TracksFragment : FunkwhaleFragment<Track, TracksAdapter>() {
  override val viewRes = R.layout.fragment_tracks
  override val recycler: RecyclerView get() = tracks

  lateinit var favoritesRepository: FavoritesRepository

  private var albumId = 0
  private var albumArtist = ""
  private var albumTitle = ""
  private var albumCover = ""

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
      .maybeLoad(maybeNormalizeUrl(albumCover))
      .noFade()
      .fit()
      .centerCrop()
      .transform(RoundedCornersTransformation(16, 0))
      .into(cover)

    artist.text = albumArtist
    title.text = albumTitle
  }

  override fun onResume() {
    super.onResume()

    GlobalScope.launch(IO) {
      RequestBus.send(Request.GetCurrentTrack).wait<Response.CurrentTrack>()?.let { response ->
        withContext(Main) {
          adapter.currentTrack = response.track
          adapter.notifyDataSetChanged()
        }
      }

      refreshDownloadedTracks()
    }

    var coverHeight: Float? = null

    scroller.setOnScrollChangeListener { _: View?, _: Int, scrollY: Int, _: Int, _: Int ->
      if (coverHeight == null) {
        coverHeight = cover.measuredHeight.toFloat()
      }

      cover.translationY = (scrollY / 2).toFloat()

      coverHeight?.let { height ->
        cover.alpha = (height - scrollY.toFloat()) / height
      }
    }

    play.setOnClickListener {
      CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))

      context.toast("All tracks were added to your queue")
    }

    context?.let { context ->
      actions.setOnClickListener {
        PopupMenu(context, actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
          inflate(R.menu.album)

          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.add_to_queue -> {
                CommandBus.send(Command.AddToQueue(adapter.data))

                context.toast("All tracks were added to your queue")
              }

              R.id.download -> CommandBus.send(Command.PinTracks(adapter.data))
            }

            true
          }

          show()
        }
      }
    }
  }

  private fun watchEventBus() {
    GlobalScope.launch(IO) {
      EventBus.get().collect { message ->
        when (message) {
          is Event.DownloadChanged -> refreshDownloadedTrack(message.download)
        }
      }
    }

    GlobalScope.launch(Main) {
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
      adapter.currentTrack = track
      adapter.notifyDataSetChanged()
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