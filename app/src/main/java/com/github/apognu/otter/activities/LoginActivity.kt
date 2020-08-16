package com.github.apognu.otter.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.LoginDialog
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.OAuth
import com.github.apognu.otter.utils.Userinfo
import com.github.apognu.otter.utils.log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.github.kittinunf.result.Result
import com.preference.PowerPreference
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

data class FwCredentials(val token: String, val non_field_errors: List<String>?)

class LoginActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_login)

    limitContainerWidth()
  }

  override fun onResume() {
    super.onResume()

    login?.setOnClickListener {
      var hostname = hostname.text.toString().trim()

      try {
        if (hostname.isEmpty()) throw Exception(getString(R.string.login_error_hostname))

        Uri.parse(hostname).apply {
          if (!cleartext.isChecked && scheme == "http") {
            throw Exception(getString(R.string.login_error_hostname_https))
          }

          if (scheme == null) {
            hostname = when (cleartext.isChecked) {
              true -> "http://$hostname"
              false -> "https://$hostname"
            }
          }
        }

        hostname_field.error = ""

        when (anonymous.isChecked) {
          false -> authedLogin(hostname)
          true -> anonymousLogin(hostname)
        }
      } catch (e: Exception) {
        val message =
          if (e.message?.isEmpty() == true) getString(R.string.login_error_hostname)
          else e.message

        hostname_field.error = message
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)

    limitContainerWidth()
  }

  private fun authedLogin(hostname: String) {
    PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).setString("hostname", hostname)

    OAuth.init(hostname)

    OAuth.register(this) {
      OAuth.authorize(this)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    data?.let {
      when (requestCode) {
        0 -> {
          OAuth.exchange(this, data,
            {
              PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).setBoolean("anonymous", false)

              lifecycleScope.launch(Main) {
                Userinfo.get(this@LoginActivity)?.let {
                  startActivity(Intent(this@LoginActivity, MainActivity::class.java))

                  return@launch finish()
                }

                throw Exception(getString(R.string.login_error_userinfo))
              }
            },
            { "error".log() }
          )
        }
      }
    }
  }

  private fun anonymousLogin(hostname: String) {
    val dialog = LoginDialog().apply {
      show(supportFragmentManager, "LoginDialog")
    }

    lifecycleScope.launch(Main) {
      try {
        val (_, _, result) = Fuel.get("$hostname/api/v1/tracks/")
          .awaitObjectResponseResult(gsonDeserializerOf(FwCredentials::class.java))

        when (result) {
          is Result.Success -> {
            PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).apply {
              setString("hostname", hostname)
              setBoolean("anonymous", true)
            }

            dialog.dismiss()
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
          }

          is Result.Failure -> {
            dialog.dismiss()

            hostname_field.error = result.error.localizedMessage
          }
        }
      } catch (e: Exception) {
        dialog.dismiss()

        val message =
          if (e.message?.isEmpty() == true) getString(R.string.login_error_hostname)
          else e.message

        hostname_field.error = message
      }
    }
  }

  private fun limitContainerWidth() {
    container.doOnLayout {
      if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && container.width >= 1440) {
        container.layoutParams.width = 1440
      } else {
        container.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
      }

      container.requestLayout()
    }
  }
}