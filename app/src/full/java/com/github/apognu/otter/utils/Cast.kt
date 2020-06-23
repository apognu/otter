package com.github.apognu.otter

import android.content.Context
import android.net.Uri
import android.view.Menu
import com.github.apognu.otter.playback.PlayerService
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.CastInterface
import com.github.apognu.otter.utils.Track
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.images.WebImage
import com.preference.PowerPreference

fun Player.onCast(): CastPlayer? {
  return if (this is CastPlayer) this
  else null
}

class Cast(val context: Context, val switchListener: PlayerService.OnPlayerSwitchListener, playerEventListener: PlayerService.PlayerEventListener) : CastInterface {
  companion object {
    fun init(context: Context) {
      CastContext.getSharedInstance(context)
    }

    fun setupButton(context: Context, menu: Menu?) {
      CastButtonFactory.setUpMediaRouteButton(context, menu, R.id.cast)
    }

    fun get(
      context: Context,
      playerSwitchListener: PlayerService.OnPlayerSwitchListener,
      playerEventListener: PlayerService.PlayerEventListener
    ): Cast = Cast(context, playerSwitchListener, playerEventListener)
  }

  private val player: Player

  init {
    player = CastPlayer(CastContext.getSharedInstance(context)).apply {
      addListener(playerEventListener)
      setSessionAvailabilityListener(CastSessionListener())
    }
  }

  override fun getPlayer(context: Context): Player = player

  override fun replaceQueue(tracks: List<Track>) {
    player.onCast()?.let { castPlayer ->
      tracks
        .map { track -> buildMediaQueueItem(track) }
        .apply {
          castPlayer.loadItems(this.toTypedArray(), 0, 0, Player.REPEAT_MODE_OFF)
          castPlayer.playWhenReady = true
        }
    }
  }

  override fun addToQueue(tracks: List<Track>) {
    player.onCast()?.let { castPlayer ->
      tracks
        .map { track -> buildMediaQueueItem(track) }
        .forEach {
          castPlayer.addItems(it)
        }
    }
  }

  override fun insertNext(track: Track, current: Int) {
    player.onCast()?.let { castPlayer ->
      val period = Timeline.Period().run {
        player.currentTimeline.getPeriod(current + 1, this)
      }

      castPlayer.addItems(period.id.toString().toInt(), buildMediaQueueItem(track))
    }
  }

  override fun remove(index: Int) {
    player.onCast()?.let { castPlayer ->
      val period = Timeline.Period().run {
        player.currentTimeline.getPeriod(index, this)
      }

      castPlayer.removeItem(period.id.toString().toInt())
    }
  }

  override fun move(oldPosition: Int, newPosition: Int) {
    player.onCast()?.let { castPlayer ->
      val period = Timeline.Period().run {
        player.currentTimeline.getPeriod(oldPosition, this)
      }

      castPlayer.moveItem(period.id.toString().toInt(), newPosition)
    }
  }

  private fun buildMediaQueueItem(track: Track): MediaQueueItem {
    val listenUrl = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")
    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("listen_token", "")
    val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
      putString(MediaMetadata.KEY_ARTIST, track.artist.name)
      putString(MediaMetadata.KEY_ALBUM_TITLE, track.album.title)
      putString(MediaMetadata.KEY_TITLE, track.title)

      addImage(WebImage(Uri.parse(mustNormalizeUrl(track.album.cover()))))
    }

    val url = Uri.parse(listenUrl)
      .buildUpon()
      .appendQueryParameter("token", token)
      .build()
      .toString()

    val mediaInfo = MediaInfo.Builder(url)
      .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
      .setMetadata(metadata)
      .build()

    return MediaQueueItem.Builder(mediaInfo).build()
  }

  inner class CastSessionListener : SessionAvailabilityListener {
    override fun onCastSessionAvailable() {
      switchListener.switchToRemote()
    }

    override fun onCastSessionUnavailable() {
      switchListener.switchToLocal()
    }
  }
}

