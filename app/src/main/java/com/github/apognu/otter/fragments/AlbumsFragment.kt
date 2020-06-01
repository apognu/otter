package com.github.apognu.otter.fragments

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.Slide
import com.github.apognu.otter.R
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.adapters.AlbumsAdapter
import com.github.apognu.otter.repositories.AlbumsRepository
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumsFragment : FunkwhaleFragment<Album, AlbumsAdapter>() {
  override val viewRes = R.layout.fragment_albums
  override val recycler: RecyclerView get() = albums

  var artistId = 0
  var artistName = ""
  var artistArt = ""

  companion object {
    fun new(artist: Artist, _art: String? = null): AlbumsFragment {
      val art = _art ?: if (artist.albums?.isNotEmpty() == true) artist.albums[0].cover.original else ""

      return AlbumsFragment().apply {
        arguments = bundleOf(
          "artistId" to artist.id,
          "artistName" to artist.name,
          "artistArt" to art
        )
      }
    }

    fun openTracks(context: Context?, album: Album, fragment: Fragment? = null) {
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
        val nextFragment = TracksFragment.new(album).apply {
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

    cover?.let { cover ->
      Picasso.get()
        .maybeLoad(maybeNormalizeUrl(artistArt))
        .noFade()
        .fit()
        .centerCrop()
        .into(cover)
    }

    cover_background?.let { background ->
      activity?.let { activity ->
        GlobalScope.launch(Dispatchers.IO) {
          val width = DisplayMetrics().apply {
            activity.windowManager.defaultDisplay.getMetrics(this)
          }.widthPixels

          val backgroundCover = Picasso.get()
            .maybeLoad(maybeNormalizeUrl(artistArt))
            .get()
            .run { Bitmap.createScaledBitmap(this, width, width, false) }
            .run { Bitmap.createBitmap(this, 0, 0, width, background.height).toDrawable(resources) }
            .apply {
              alpha = 20
              gravity = Gravity.CENTER
            }

          withContext(Dispatchers.Main) {
            background.background = backgroundCover
          }
        }
      }
    }

    artist.text = artistName
  }

  inner class OnAlbumClickListener : AlbumsAdapter.OnAlbumClickListener {
    override fun onClick(view: View?, album: Album) {
      openTracks(context, album, fragment = this@AlbumsFragment)
    }
  }
}