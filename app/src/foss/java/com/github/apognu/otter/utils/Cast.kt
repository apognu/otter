package com.github.apognu.otter

import android.content.Context
import android.view.Menu
import com.github.apognu.otter.playback.PlayerService
import com.github.apognu.otter.utils.CastInterface

class Cast(val context: Context, val switchListener: PlayerService.OnPlayerSwitchListener, playerEventListener: PlayerService.PlayerEventListener) : CastInterface {
  companion object {
    fun init(context: Context) {}
    fun setupButton(context: Context, menu: Menu?) {}

    fun get(
      context: Context,
      playerSwitchListener: PlayerService.OnPlayerSwitchListener,
      playerEventListener: PlayerService.PlayerEventListener
    ): Cast? = null
  }
}
