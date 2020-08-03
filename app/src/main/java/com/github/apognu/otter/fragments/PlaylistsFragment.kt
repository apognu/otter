package com.github.apognu.otter.fragments

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.PlaylistsAdapter
import com.github.apognu.otter.models.api.FunkwhalePlaylist
import com.github.apognu.otter.models.dao.PlaylistEntity
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.PlaylistsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.viewmodels.PlaylistsViewModel
import kotlinx.android.synthetic.main.fragment_playlists.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PlaylistsFragment : OtterFragment<FunkwhalePlaylist, PlaylistEntity, PlaylistsAdapter>() {
  override val repository by inject<PlaylistsRepository>()
  override val adapter by inject<PlaylistsAdapter> { parametersOf(context, OnPlaylistClickListener()) }
  override val viewModel by viewModel<PlaylistsViewModel>()
  override val liveData by lazy { viewModel.playlists }

  override val viewRes = R.layout.fragment_playlists
  override val recycler: RecyclerView get() = playlists
  override val alwaysRefresh = false

  private val favoritesRepository by inject<FavoritesRepository>()

  inner class OnPlaylistClickListener : PlaylistsAdapter.OnPlaylistClickListener {
    override fun onClick(holder: View?, playlist: PlaylistEntity) {
      (context as? MainActivity)?.let { activity ->
        exitTransition = Fade().apply {
          duration = AppContext.TRANSITION_DURATION
          interpolator = AccelerateDecelerateInterpolator()

          view?.let {
            addTarget(it)
          }
        }

        val fragment = PlaylistTracksFragment.new(playlist, favoritesRepository).apply {
          enterTransition = Slide().apply {
            duration = AppContext.TRANSITION_DURATION
            interpolator = AccelerateDecelerateInterpolator()
          }
        }

        activity.supportFragmentManager
          .beginTransaction()
          .replace(R.id.container, fragment)
          .addToBackStack(null)
          .commit()
      }
    }
  }
}