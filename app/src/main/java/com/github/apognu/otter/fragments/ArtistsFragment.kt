package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.ArtistsAdapter
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Artist
import com.github.apognu.otter.utils.onViewPager
import kotlinx.android.synthetic.main.fragment_artists.*

class ArtistsFragment : FunkwhaleFragment<Artist, ArtistsAdapter>() {
  override val viewRes = R.layout.fragment_artists
  override val recycler: RecyclerView get() = artists

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = ArtistsAdapter(context, OnArtistClickListener())
    repository = ArtistsRepository(context)
  }

  inner class OnArtistClickListener : ArtistsAdapter.OnArtistClickListener {
    override fun onClick(holder: View?, artist: Artist) {
      (context as? MainActivity)?.let { activity ->
        onViewPager {
          exitTransition = Fade().apply {
            duration = AppContext.TRANSITION_DURATION
            interpolator = AccelerateDecelerateInterpolator()

            view?.let {
              addTarget(it)
            }
          }
        }

        val fragment = AlbumsFragment.new(artist).apply {
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
