package com.github.apognu.otter.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.BrowseFragment
import com.github.apognu.otter.fragments.QueueFragment
import com.github.apognu.otter.playback.MediaControlsManager
import com.github.apognu.otter.playback.PlayerService
import com.github.apognu.otter.repositories.FavoritedRepository
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.*
import com.preference.PowerPreference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.partial_now_playing.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
  enum class ResultCode(val code: Int) {
    LOGOUT(1001)
  }

  private val favoriteRepository = FavoritesRepository(this)
  private val favoriteCheckRepository = FavoritedRepository(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    AppContext.init(this)

    setContentView(R.layout.activity_main)
    setSupportActionBar(appbar)

    when (intent.action) {
      MediaControlsManager.NOTIFICATION_ACTION_OPEN_QUEUE.toString() -> launchDialog(QueueFragment())
    }

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.container, BrowseFragment())
      .commit()

    startService(Intent(this, PlayerService::class.java))

    watchEventBus()

    CommandBus.send(Command.RefreshService)
  }

  override fun onResume() {
    super.onResume()

    now_playing_toggle.setOnClickListener {
      CommandBus.send(Command.ToggleState)
    }

    now_playing_next.setOnClickListener {
      CommandBus.send(Command.NextTrack)
    }

    now_playing_details_previous.setOnClickListener {
      CommandBus.send(Command.PreviousTrack)
    }

    now_playing_details_next.setOnClickListener {
      CommandBus.send(Command.NextTrack)
    }

    now_playing_details_toggle.setOnClickListener {
      CommandBus.send(Command.ToggleState)
    }

    now_playing_details_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onStopTrackingTouch(view: SeekBar?) {}

      override fun onStartTrackingTouch(view: SeekBar?) {}

      override fun onProgressChanged(view: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
          CommandBus.send(Command.Seek(progress))
        }
      }
    })
  }

  override fun onBackPressed() {
    if (now_playing.isOpened()) {
      now_playing.close()
      return
    }

    super.onBackPressed()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.toolbar, menu)

    // CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.cast)

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        now_playing.close()

        (supportFragmentManager.fragments.last() as? BrowseFragment)?.let {
          it.selectTabAt(0)

          return true
        }

        launchFragment(BrowseFragment())
      }

      R.id.nav_queue -> launchDialog(QueueFragment())
      R.id.nav_search -> startActivity(Intent(this, SearchActivity::class.java))
      R.id.settings -> startActivityForResult(Intent(this, SettingsActivity::class.java), 0)
    }

    return true
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == ResultCode.LOGOUT.code) {
      Intent(this, LoginActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        startActivity(this)
        finish()
      }
    }
  }

  private fun launchFragment(fragment: Fragment) {
    supportFragmentManager.fragments.lastOrNull()?.also { oldFragment ->
      oldFragment.enterTransition = null
      oldFragment.exitTransition = null

      supportFragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    supportFragmentManager
      .beginTransaction()
      .setCustomAnimations(0, 0, 0, 0)
      .replace(R.id.container, fragment)
      .commit()
  }

  private fun launchDialog(fragment: DialogFragment) {
    supportFragmentManager.beginTransaction().let {
      fragment.show(it, "")
    }
  }

  @SuppressLint("NewApi")
  private fun watchEventBus() {
    GlobalScope.launch(Main) {
      for (message in EventBus.asChannel<Event>()) {
        when (message) {
          is Event.LogOut -> {
            PowerPreference.clearAllData()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            })

            finish()
          }

          is Event.PlaybackError -> toast(message.message)

          is Event.Buffering -> {
            when (message.value) {
              true -> now_playing_buffering.visibility = View.VISIBLE
              false -> now_playing_buffering.visibility = View.GONE
            }
          }

          is Event.PlaybackStopped -> {
            if (now_playing.visibility == View.VISIBLE) {
              (container.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                it.bottomMargin = it.bottomMargin / 2
              }

              now_playing.animate()
                .alpha(0.0f)
                .setDuration(400)
                .setListener(object : AnimatorListenerAdapter() {
                  override fun onAnimationEnd(animator: Animator?) {
                    now_playing.visibility = View.GONE
                  }
                })
                .start()
            }
          }

          is Event.TrackPlayed -> {
            message.track?.let { track ->
              if (now_playing.visibility == View.GONE) {
                now_playing.visibility = View.VISIBLE
                now_playing.alpha = 0f

                now_playing.animate()
                  .alpha(1.0f)
                  .setDuration(400)
                  .setListener(null)
                  .start()

                (container.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                  it.bottomMargin = it.bottomMargin * 2
                }
              }

              now_playing_title.text = track.title
              now_playing_album.text = track.artist.name
              now_playing_toggle.icon = getDrawable(R.drawable.pause)
              now_playing_progress.progress = 0

              now_playing_details_title.text = track.title
              now_playing_details_artist.text = track.artist.name
              now_playing_details_toggle.icon = getDrawable(R.drawable.pause)
              now_playing_details_progress.progress = 0

              Picasso.get()
                .maybeLoad(maybeNormalizeUrl(track.album.cover.original))
                .fit()
                .centerCrop()
                .into(now_playing_cover)

              Picasso.get()
                .maybeLoad(maybeNormalizeUrl(track.album.cover.original))
                .fit()
                .centerCrop()
                .into(now_playing_details_cover)

              favoriteCheckRepository.fetch().untilNetwork(IO) { favorites ->
                GlobalScope.launch(Main) {
                  track.favorite = favorites.contains(track.id)

                  when (track.favorite) {
                    true -> now_playing_details_favorite.setColorFilter(getColor(R.color.colorFavorite))
                    false -> now_playing_details_favorite.setColorFilter(getColor(R.color.controlForeground))
                  }
                }
              }

              now_playing_details_favorite.setOnClickListener {
                when (track.favorite) {
                  true -> {
                    favoriteRepository.deleteFavorite(track.id)
                    now_playing_details_favorite.setColorFilter(getColor(R.color.controlForeground))
                  }

                  false -> {
                    favoriteRepository.addFavorite(track.id)
                    now_playing_details_favorite.setColorFilter(getColor(R.color.colorFavorite))
                  }
                }

                track.favorite = !track.favorite

                favoriteRepository.fetch(Repository.Origin.Network.origin)
              }
            }
          }

          is Event.StateChanged -> {
            when (message.playing) {
              true -> {
                now_playing_toggle.icon = getDrawable(R.drawable.pause)
                now_playing_details_toggle.icon = getDrawable(R.drawable.pause)
              }

              false -> {
                now_playing_toggle.icon = getDrawable(R.drawable.play)
                now_playing_details_toggle.icon = getDrawable(R.drawable.play)
              }
            }
          }

          is Event.QueueChanged -> {
            findViewById<View>(R.id.nav_queue)?.let { view ->
              ObjectAnimator.ofFloat(view, View.ROTATION, 0f, 360f).let {
                it.duration = 500
                it.interpolator = AccelerateDecelerateInterpolator()
                it.start()
              }
            }
          }
        }
      }
    }

    GlobalScope.launch(Main) {
      for ((current, duration, percent) in ProgressBus.asChannel()) {
        now_playing_progress.progress = percent
        now_playing_details_progress.progress = percent

        val currentMins = (current / 1000) / 60
        val currentSecs = (current / 1000) % 60

        val durationMins = duration / 60
        val durationSecs = duration % 60

        now_playing_details_progress_current.text = "%02d:%02d".format(currentMins, currentSecs)
        now_playing_details_progress_duration.text = "%02d:%02d".format(durationMins, durationSecs)
      }
    }
  }
}
