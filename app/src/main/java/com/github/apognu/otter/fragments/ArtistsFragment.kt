package com.github.apognu.otter.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.ArtistsAdapter
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.repositories.ArtistsRepository
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.onViewPager
import com.github.apognu.otter.viewmodels.ArtistsViewModel
import kotlinx.android.synthetic.main.fragment_artists.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ArtistsFragment : PagedOtterFragment<FunkwhaleArtist, Artist, ArtistsAdapter>() {
  override val repository by inject<ArtistsRepository>()
  override val adapter by inject<ArtistsAdapter> { parametersOf(context, OnArtistClickListener()) }
  override val viewModel by viewModel<ArtistsViewModel>()

  override val liveData by lazy { viewModel.artistsPaged }
  override val viewRes = R.layout.fragment_artists
  override val recycler: RecyclerView get() = artists

  companion object {
    fun openAlbums(context: Context?, artist: Artist?, fragment: Fragment? = null, art: String? = null) {
      artist?.let {
        (context as? MainActivity)?.let {
          fragment?.let { fragment ->
            fragment.onViewPager {
              exitTransition = Fade().apply {
                duration = AppContext.TRANSITION_DURATION
                interpolator = AccelerateDecelerateInterpolator()

                view?.let {
                  addTarget(it)
                }
              }
            }
          }
        }

        (context as? AppCompatActivity)?.let { activity ->
          val nextFragment = AlbumsFragment.new(artist, art).apply {
            enterTransition = Slide().apply {
              duration = AppContext.TRANSITION_DURATION
              interpolator = AccelerateDecelerateInterpolator()
            }
          }

          activity.supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, nextFragment)
            .addToBackStack(null)
            .commit()
        }
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_artists, container, false)
  }

  inner class OnArtistClickListener : ArtistsAdapter.OnArtistClickListener {
    override fun onClick(holder: View?, artist: Artist) {
      openAlbums(context, artist, this@ArtistsFragment, artist.album_cover)
    }
  }
}
