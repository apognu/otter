package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.AlbumsGridAdapter
import com.github.apognu.otter.repositories.AlbumsRepository
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.AppContext
import kotlinx.android.synthetic.main.fragment_albums_grid.*

class AlbumsGridFragment : OtterFragment<Album, AlbumsGridAdapter>() {
  override val viewRes = R.layout.fragment_albums_grid
  override val recycler: RecyclerView get() = albums
  override val layoutManager get() = GridLayoutManager(context, 3)
  override val alwaysRefresh = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = AlbumsGridAdapter(context, OnAlbumClickListener())
    repository = AlbumsRepository(context)
  }

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