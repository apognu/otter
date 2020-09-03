package com.github.apognu.otter.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.*
import com.github.apognu.otter.playback.MediaControlsManager
import com.github.apognu.otter.playback.PinService
import com.github.apognu.otter.playback.PlayerService
import com.github.apognu.otter.repositories.FavoritedRepository
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.views.DisableableFrameLayout
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.DownloadService
import com.google.gson.Gson
import com.preference.PowerPreference
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.partial_now_playing.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
  enum class ResultCode(val code: Int) {
    LOGOUT(1001)
  }

  private val favoriteRepository = FavoritesRepository(this)
  private val favoritedRepository = FavoritedRepository(this)
  private var menu: Menu? = null

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

    watchEventBus()
  }

  override fun onResume() {
    super.onResume()

    (container as? DisableableFrameLayout)?.setShouldRegisterTouch { _ ->
      if (now_playing.isOpened()) {
        now_playing.close()

        return@setShouldRegisterTouch false
      }

      true
    }

    favoritedRepository.update(this, lifecycleScope)

    startService(Intent(this, PlayerService::class.java))
    DownloadService.start(this, PinService::class.java)

    CommandBus.send(Command.RefreshService)

    lifecycleScope.launch(IO) {
      Userinfo.get()
    }

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

    landscape_queue?.let {
      supportFragmentManager.beginTransaction().replace(R.id.landscape_queue, LandscapeQueueFragment()).commit()
    }
  }

  override fun onBackPressed() {
    if (now_playing.isOpened()) {
      now_playing.close()
      return
    }

    super.onBackPressed()
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    this.menu = menu

    return super.onPrepareOptionsMenu(menu)
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.toolbar, menu)

    menu?.findItem(R.id.nav_all_music)?.isChecked = Settings.getScopes().contains("all")
    menu?.findItem(R.id.nav_my_music)?.isChecked = Settings.getScopes().contains("me")
    menu?.findItem(R.id.nav_followed)?.isChecked = Settings.getScopes().contains("subscribed")

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
      R.id.nav_all_music, R.id.nav_my_music, R.id.nav_followed -> {
        menu?.let { menu ->
          item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
          item.actionView = View(this)
          item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?) = false
            override fun onMenuItemActionCollapse(item: MenuItem?) = false
          })

          item.isChecked = !item.isChecked

          val scopes = Settings.getScopes().toMutableSet()

          val new = when (item.itemId) {
            R.id.nav_my_music -> "me"
            R.id.nav_followed -> "subscribed"

            else -> {
              menu.findItem(R.id.nav_my_music).isChecked = false
              menu.findItem(R.id.nav_followed).isChecked = false

              PowerPreference.getDefaultFile().set("scope", "all")
              EventBus.send(Event.ListingsChanged)

              return false
            }
          }

          menu.findItem(R.id.nav_all_music).isChecked = false

          scopes.remove("all")

          when (item.isChecked) {
            true -> scopes.add(new)
            false -> scopes.remove(new)
          }

          PowerPreference.getDefaultFile().set("scope", scopes.joinToString(","))
          EventBus.send(Event.ListingsChanged)

          return false
        }
      }
      R.id.nav_downloads -> startActivity(Intent(this, DownloadsActivity::class.java))
      R.id.settings -> startActivityForResult(Intent(this, SettingsActivity::class.java), 0)
    }

    return true
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == ResultCode.LOGOUT.code) {
      Intent(this, LoginActivity::class.java).apply {
        Otter.get().deleteAllData()

        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        stopService(Intent(this@MainActivity, PlayerService::class.java))
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
    lifecycleScope.launch(Main) {
      EventBus.get().collect { message ->
        when (message) {
          is Event.LogOut -> {
            Otter.get().deleteAllData()

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

              landscape_queue?.let { landscape_queue ->
                (landscape_queue.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
                  it.bottomMargin = it.bottomMargin / 2
                }
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

          is Event.TrackFinished -> incrementListenCount(message.track)

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

    lifecycleScope.launch(Main) {
      CommandBus.get().collect { command ->
        when (command) {
          is Command.StartService -> {
            Build.VERSION_CODES.O.onApi(
              {
                startForegroundService(Intent(this@MainActivity, PlayerService::class.java).apply {
                  putExtra(PlayerService.INITIAL_COMMAND_KEY, command.command.toString())
                })
              },
              {
                startService(Intent(this@MainActivity, PlayerService::class.java).apply {
                  putExtra(PlayerService.INITIAL_COMMAND_KEY, command.command.toString())
                })
              }
            )
          }

          is Command.RefreshTrack -> refreshCurrentTrack(command.track)
        }
      }
    }

    lifecycleScope.launch(Main) {
      ProgressBus.get().collect { (current, duration, percent) ->
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

  private fun refreshCurrentTrack(track: Track?) {
    track?.let {
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

        landscape_queue?.let { landscape_queue ->
          (landscape_queue.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.bottomMargin = it.bottomMargin * 2
          }
        }
      }

      now_playing_title.text = track.title
      now_playing_album.text = track.artist.name
      now_playing_toggle.icon = getDrawable(R.drawable.pause)

      now_playing_details_title.text = track.title
      now_playing_details_artist.text = track.artist.name
      now_playing_details_toggle.icon = getDrawable(R.drawable.pause)

      Picasso.get()
        .maybeLoad(maybeNormalizeUrl(track.album?.cover?.urls?.original))
        .fit()
        .centerCrop()
        .into(now_playing_cover)

      now_playing_details_cover?.let { now_playing_details_cover ->
        Picasso.get()
          .maybeLoad(maybeNormalizeUrl(track.album?.cover()))
          .fit()
          .centerCrop()
          .transform(RoundedCornersTransformation(16, 0))
          .into(now_playing_details_cover)
      }

      if (now_playing_details_cover == null) {
        lifecycleScope.launch(Default) {
          val width = DisplayMetrics().apply {
            windowManager.defaultDisplay.getMetrics(this)
          }.widthPixels

          val backgroundCover = Picasso.get()
            .maybeLoad(maybeNormalizeUrl(track.album?.cover()))
            .get()
            .run { Bitmap.createScaledBitmap(this, width, width, false).toDrawable(resources) }
            .apply {
              alpha = 20
              gravity = Gravity.CENTER
            }

          withContext(Main) {
            now_playing_details.background = backgroundCover
          }
        }
      }

      now_playing_details_repeat?.let { now_playing_details_repeat ->
        changeRepeatMode(Cache.get(this@MainActivity, "repeat")?.readLine()?.toInt() ?: 0)

        now_playing_details_repeat.setOnClickListener {
          val current = Cache.get(this@MainActivity, "repeat")?.readLine()?.toInt() ?: 0

          changeRepeatMode((current + 1) % 3)
        }
      }

      now_playing_details_info?.let { now_playing_details_info ->
        now_playing_details_info.setOnClickListener {
          PopupMenu(this@MainActivity, now_playing_details_info, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
            inflate(R.menu.track_info)

            setOnMenuItemClickListener {
              when (it.itemId) {
                R.id.track_info_artist -> ArtistsFragment.openAlbums(this@MainActivity, track.artist, art = track.album?.cover())
                R.id.track_info_album -> AlbumsFragment.openTracks(this@MainActivity, track.album)
                R.id.track_info_details -> TrackInfoDetailsFragment.new(track).show(supportFragmentManager, "dialog")
              }

              now_playing.close()

              true
            }

            show()
          }
        }
      }

      now_playing_details_favorite?.let { now_playing_details_favorite ->
        favoritedRepository.fetch().untilNetwork(lifecycleScope, IO) { favorites, _, _, _ ->
          lifecycleScope.launch(Main) {
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
  }

  private fun changeRepeatMode(index: Int) {
    when (index) {
      // From no repeat to repeat all
      0 -> {
        Cache.set(this@MainActivity, "repeat", "0".toByteArray())

        now_playing_details_repeat?.setImageResource(R.drawable.repeat)
        now_playing_details_repeat?.setColorFilter(ContextCompat.getColor(this, R.color.controlForeground))
        now_playing_details_repeat?.alpha = 0.2f

        CommandBus.send(Command.SetRepeatMode(Player.REPEAT_MODE_OFF))
      }

      // From repeat all to repeat one
      1 -> {
        Cache.set(this@MainActivity, "repeat", "1".toByteArray())

        now_playing_details_repeat?.setImageResource(R.drawable.repeat)
        now_playing_details_repeat?.setColorFilter(ContextCompat.getColor(this, R.color.controlForeground))
        now_playing_details_repeat?.alpha = 1.0f

        CommandBus.send(Command.SetRepeatMode(Player.REPEAT_MODE_ALL))
      }

      // From repeat one to no repeat
      2 -> {
        Cache.set(this@MainActivity, "repeat", "2".toByteArray())
        now_playing_details_repeat?.setImageResource(R.drawable.repeat_one)
        now_playing_details_repeat?.setColorFilter(ContextCompat.getColor(this, R.color.controlForeground))
        now_playing_details_repeat?.alpha = 1.0f

        CommandBus.send(Command.SetRepeatMode(Player.REPEAT_MODE_ONE))
      }
    }
  }

  private fun incrementListenCount(track: Track?) {
    track?.let {
      lifecycleScope.launch(IO) {
        try {
          Fuel
            .post(mustNormalizeUrl("/api/v1/history/listenings/"))
            .authorize()
            .header("Content-Type", "application/json")
            .body(Gson().toJson(mapOf("track" to track.id)))
            .awaitStringResponse()
        } catch (_: Exception) {
        }
      }
    }
  }
}
