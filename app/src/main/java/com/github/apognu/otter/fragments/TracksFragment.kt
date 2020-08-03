package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.TracksRepository
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.CommandBus
import com.github.apognu.otter.utils.maybeLoad
import com.github.apognu.otter.utils.toast
import com.github.apognu.otter.viewmodels.TracksViewModel
import com.preference.PowerPreference
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.fragment_tracks.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TracksFragment : OtterFragment<FunkwhaleTrack, Track, TracksAdapter>() {
  private val favoritesRepository by inject<FavoritesRepository>()

  override val repository by inject<TracksRepository> { parametersOf(albumId) }
  override val adapter by inject<TracksAdapter> { parametersOf(context, FavoriteListener()) }
  override val viewModel by viewModel<TracksViewModel> { parametersOf(albumId) }
  override val liveData by lazy { viewModel.tracks }

  override val viewRes = R.layout.fragment_tracks
  override val recycler: RecyclerView get() = tracks

  private var albumId = 0

  companion object {
    fun new(album: Album): TracksFragment {
      return TracksFragment().apply {
        arguments = bundleOf("albumId" to album.id)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    arguments?.apply {
      albumId = getInt("albumId")
    }
  }

  override fun onResume() {
    super.onResume()

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

    when (PowerPreference.getDefaultFile().getString("play_order")) {
      "in_order" -> play.text = getString(R.string.playback_play)
      else -> play.text = getString(R.string.playback_shuffle)
    }

    play.setOnClickListener {
      when (PowerPreference.getDefaultFile().getString("play_order")) {
        "in_order" -> CommandBus.send(Command.ReplaceQueue(adapter.data))
        else -> CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))
      }

      context.toast("All tracks were added to your queue")
    }

    context?.let { context ->
      actions.setOnClickListener {
        PopupMenu(context, actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
          inflate(R.menu.album)

          menu.findItem(R.id.play_secondary)?.let { item ->
            when (PowerPreference.getDefaultFile().getString("play_order")) {
              "in_order" -> item.title = getString(R.string.playback_shuffle)
              else -> item.title = getString(R.string.playback_play)
            }
          }

          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.play_secondary -> when (PowerPreference.getDefaultFile().getString("play_order")) {
                "in_order" -> CommandBus.send(Command.ReplaceQueue(adapter.data.shuffled()))
                else -> CommandBus.send(Command.ReplaceQueue(adapter.data))
              }

              R.id.add_to_queue -> {
                when (PowerPreference.getDefaultFile().getString("play_order")) {
                  "in_order" -> CommandBus.send(Command.AddToQueue(adapter.data))
                  else -> CommandBus.send(Command.AddToQueue(adapter.data.shuffled()))
                }

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

  override fun onDataUpdated(data: List<Track>?) {
    data?.let {
      title.text = data.getOrNull(0)?.album?.title
      artist.text = data.getOrNull(0)?.artist?.name

      Picasso.get()
        .maybeLoad(data.getOrNull(0)?.album?.cover)
        .noFade()
        .fit()
        .centerCrop()
        .transform(RoundedCornersTransformation(16, 0))
        .into(cover)
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