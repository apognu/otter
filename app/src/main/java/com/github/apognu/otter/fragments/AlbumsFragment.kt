package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.AlbumsAdapter
import com.github.apognu.otter.repositories.AlbumsRepository
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Artist
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_albums.*

class AlbumsFragment : FunkwhaleFragment<Album, AlbumsAdapter>() {
  override val viewRes = R.layout.fragment_albums
  override val recycler: RecyclerView get() = albums

  var artistId = 0
  var artistName = ""
  var artistArt = ""

  companion object {
    fun new(artist: Artist): AlbumsFragment {
      return AlbumsFragment().apply {
        arguments = bundleOf(
          "artistId" to artist.id,
          "artistName" to artist.name,
          "artistArt" to artist.albums!![0].cover.original
        )
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    arguments?.apply {
      artistId = getInt("artistId")
      artistName = getString("artistName") ?: ""
      artistArt = getString("artistArt") ?: ""
    }

    adapter = AlbumsAdapter(context, OnAlbumClickListener())
    repository = AlbumsRepository(context, artistId)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    Picasso.get()
      .load(artistArt)
      .noFade()
      .fit()
      .centerCrop()
      .into(cover)

    artist.text = artistName
  }

  inner class OnAlbumClickListener : AlbumsAdapter.OnAlbumClickListener {
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