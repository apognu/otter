package com.github.apognu.otter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.preference.PowerPreference

class Otter : Application() {
  override fun onCreate() {
    super.onCreate()

    when (PowerPreference.getDefaultFile().getString("night_mode")) {
      "on" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      "off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
  }
}