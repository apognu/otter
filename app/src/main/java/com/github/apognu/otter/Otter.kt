package com.github.apognu.otter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.Request
import com.preference.PowerPreference
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

class Otter : Application() {
  companion object {
    private var instance: Otter = Otter()

    fun get(): Otter = instance
  }

  val eventBus: BroadcastChannel<Event> = BroadcastChannel(10)
  val commandBus: Channel<Command> = Channel(10)
  val requestBus: BroadcastChannel<Request> = BroadcastChannel(10)
  val progressBus: BroadcastChannel<Triple<Int, Int, Int>> = ConflatedBroadcastChannel()

  override fun onCreate() {
    super.onCreate()

    instance = this

    when (PowerPreference.getDefaultFile().getString("night_mode")) {
      "on" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      "off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
  }
}