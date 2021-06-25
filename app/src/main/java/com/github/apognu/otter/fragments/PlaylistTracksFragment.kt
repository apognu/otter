package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.PlaylistTracksAdapter
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.ManagementPlaylistsRepository
import com.github.apognu.otter.repositories.PlaylistTracksRepository
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_tracks.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PlaylistTracksFragment : OtterFragment<PlaylistTrack, PlaylistTracksAdapter>() {
  override val viewRes = R.layout.fragment_tracks
  override val recycler: RecyclerView get() = tracks

  lateinit var favoritesRepository: FavoritesRepository
  lateinit var playlistsRepository: ManagementPlaylistsRepository

  var albumId = 0
  var albumArtist = ""
  var albumTitle = ""
  var albumCover = ""

  companion object {
    fun new(playlist: Playlist): PlaylistTracksFragment {
      return PlaylistTracksFragment().apply {
        arguments = bundleOf(
          "albumId" to playlist.id,
          "albumArtist" to "N/A",
          "albumTitle" to playlist.name,
          "albumCover" to ""
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

    adapter = PlaylistTracksAdapter(context, FavoriteListener(), PlaylistListener())
    repository = PlaylistTracksRepository(context, albumId)
    favoritesRepository = FavoritesRepository(context)
    playlistsRepository = ManagementPlaylistsRepository(context)

    watchEventBus()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    cover.visibility = View.INVISIBLE
    covers.visibility = View.VISIBLE

    artist.text = "Playlist"
    title.text = albumTitle
  }

  override fun onResume() {
    super.onResume()

    lifecycleScope.launch(Main) {
      RequestBus.send(Request.GetCurrentTrack).wait<Response.CurrentTrack>()?.let { response ->
        adapter.currentTrack = response.track
        adapter.notifyDataSetChanged()
      }
    }

    var coverHeight: Float? = null

    scroller.setOnScrollChangeListener { _: View?, _: Int, scrollY: Int, _: Int, _: Int ->
      if (coverHeight == null) {
        coverHeight = covers.measuredHeight.toFloat()
      }

      covers.translationY = (scrollY / 2).toFloat()

      coverHeight?.let { height ->
        covers.alpha = (height - scrollY.toFloat()) / height
      }
    }

    play.setOnClickListener {
      CommandBus.send(Command.ReplaceQueue(adapter.data.map { it.track }.shuffled()))

      context.toast("All tracks were added to your queue")
    }

    context?.let { context ->
      actions.setOnClickListener {
        PopupMenu(context, actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
          inflate(R.menu.album)

          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.add_to_queue -> {
                CommandBus.send(Command.AddToQueue(adapter.data.map { it.track }))

                context.toast("All tracks were added to your queue")
              }

              R.id.download -> CommandBus.send(Command.PinTracks(adapter.data.map { it.track }))
            }

            true
          }

          show()
        }
      }
    }
  }

  override fun onDataFetched(data: List<PlaylistTrack>) {
    data.map { it.track.album }.toSet().map { it?.cover() }.take(4).forEachIndexed { index, url ->
      val imageView = when (index) {
        0 -> cover_top_left
        1 -> cover_top_right
        2 -> cover_bottom_left
        3 -> cover_bottom_right
        else -> cover_top_left
      }

      val corner = when (index) {
        0 -> RoundedCornersTransformation.CornerType.TOP_LEFT
        1 -> RoundedCornersTransformation.CornerType.TOP_RIGHT
        2 -> RoundedCornersTransformation.CornerType.BOTTOM_LEFT
        3 -> RoundedCornersTransformation.CornerType.BOTTOM_RIGHT
        else -> RoundedCornersTransformation.CornerType.TOP_LEFT
      }

      imageView?.let { view ->
        lifecycleScope.launch(Main) {
          Picasso.get()
            .maybeLoad(maybeNormalizeUrl(url))
            .fit()
            .centerCrop()
            .transform(RoundedCornersTransformation(16, 0, corner))
            .into(view)
        }
      }
    }
  }

  private fun watchEventBus() {
    lifecycleScope.launch(Main) {
      CommandBus.get().collect { command ->
        when (command) {
          is Command.RefreshTrack -> refreshCurrentTrack(command.track)
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

  inner class FavoriteListener : PlaylistTracksAdapter.OnFavoriteListener {
    override fun onToggleFavorite(id: Int, state: Boolean) {
      when (state) {
        true -> favoritesRepository.addFavorite(id)
        false -> favoritesRepository.deleteFavorite(id)
      }
    }
  }

  inner class PlaylistListener : PlaylistTracksAdapter.OnPlaylistListener {
    override fun onMoveTrack(from: Int, to: Int) {
      playlistsRepository.move(albumId, from, to)
    }

    override fun onRemoveTrackFromPlaylist(track: Track, index: Int) {
      lifecycleScope.launch(Main) {
        playlistsRepository.remove(albumId, track, index)
        update()
      }
    }
  }
}
