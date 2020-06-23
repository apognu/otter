package com.github.apognu.otter.utils

import android.content.Context
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer

interface CastInterface {
  fun isCastSessionAvailable(): Boolean = false
  fun getPlayer(context: Context): Player = SimpleExoPlayer.Builder(context).build()

  fun replaceQueue(tracks: List<Track>) {}
  fun addToQueue(tracks: List<Track>) {}
  fun insertNext(track: Track, current: Int) {}
  fun remove(index: Int) {}
  fun move(oldPosition: Int, newPosition: Int) {}
}