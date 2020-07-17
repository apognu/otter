package com.github.apognu.otter.fragments

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.AlbumsGridAdapter
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.domain.Album
import com.github.apognu.otter.repositories.AlbumsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.viewmodels.AlbumsViewModel
import kotlinx.android.synthetic.main.fragment_albums_grid.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class AlbumsGridFragment : LiveOtterFragment<FunkwhaleAlbum, Album, AlbumsGridAdapter>() {
  override val repository by inject<AlbumsRepository> { parametersOf(null) }
  override val adapter by inject<AlbumsGridAdapter> { parametersOf(context, OnAlbumClickListener()) }
  override val viewModel by viewModel<AlbumsViewModel> { parametersOf(null) }
  override val liveData by lazy { viewModel.albums }

  override val viewRes = R.layout.fragment_albums_grid
  override val recycler: RecyclerView get() = albums
  override val layoutManager get() = GridLayoutManager(context, 3)
  override val alwaysRefresh = false

  inner class OnAlbumClickListener : AlbumsGridAdapter.OnAlbumClickListener {
    override fun onClick(view: View?, album: Album) {
      (context as? MainActivity)?.let { activity ->
        exitTransition = Fade().apply {
          duration = AppContext.TRANSITION_DURATION
          interpolator = AccelerateDecelerateInterpolator()

          view?.let {
            addTarget(it)
          }
        }

        val fragment = TracksFragment.new(album).apply {
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