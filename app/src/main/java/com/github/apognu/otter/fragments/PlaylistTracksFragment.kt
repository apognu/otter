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
import com.github.apognu.otter.models.api.FunkwhalePlaylistTrack
import com.github.apognu.otter.models.dao.PlaylistEntity
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.PlaylistTracksRepository
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.viewmodels.PlaylistViewModel
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_tracks.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PlaylistTracksFragment : OtterFragment<FunkwhalePlaylistTrack, Track, PlaylistTracksAdapter>() {
  private val favoritesRepository by inject<FavoritesRepository>()
  override val repository by inject<PlaylistTracksRepository> { parametersOf(playlistId) }
  override val adapter by inject<PlaylistTracksAdapter> { parametersOf(context, FavoriteListener()) }
  override val viewModel by viewModel<PlaylistViewModel> { parametersOf(playlistId) }
  override val liveData by lazy { viewModel.tracks }

  override val viewRes = R.layout.fragment_tracks
  override val recycler: RecyclerView get() = tracks

  var playlistId = 0
  var playlistName = ""

  companion object {
    fun new(playlist: PlaylistEntity, favoritesRepository: FavoritesRepository): PlaylistTracksFragment {
      return PlaylistTracksFragment().apply {
        arguments = bundleOf(
          "playlistId" to playlist.id,
          "playlistName" to playlist.name
        )
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    arguments?.apply {
      playlistId = getInt("playlistId")
      playlistName = getString("playlistName") ?: "N/A"
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    cover.visibility = View.INVISIBLE
    covers.visibility = View.VISIBLE

    artist.text = "Playlist"
    title.text = playlistName
  }

  override fun onResume() {
    super.onResume()

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

  override fun onDataFetched(data: List<FunkwhalePlaylistTrack>) {
    data.map { it.track.album }.toSet().map { it?.cover?.urls?.original }.take(4).forEachIndexed { index, url ->
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

  inner class FavoriteListener : PlaylistTracksAdapter.OnFavoriteListener {
    override fun onToggleFavorite(id: Int, state: Boolean) {
      when (state) {
        true -> favoritesRepository.addFavorite(id)
        false -> favoritesRepository.deleteFavorite(id)
      }
    }
  }
}