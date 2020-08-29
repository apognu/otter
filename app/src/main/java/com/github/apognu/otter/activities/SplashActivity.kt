package com.github.apognu.otter.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.apognu.otter.Otter
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Settings

class SplashActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    getSharedPreferences(AppContext.PREFS_CREDENTIALS, Context.MODE_PRIVATE).apply {
      when (Settings.hasAccessToken() || Settings.isAnonymous()) {
        true -> Intent(this@SplashActivity, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NO_ANIMATION

          startActivity(this)
        }

        false -> Intent(this@SplashActivity, LoginActivity::class.java).apply {
          Otter.get().deleteAllData()

          flags = Intent.FLAG_ACTIVITY_NO_ANIMATION

          startActivity(this)
        }
      }
    }
  }
}