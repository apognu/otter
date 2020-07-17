package com.github.apognu.otter.activities

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.LoginDialog
import com.github.apognu.otter.models.api.Credentials
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Userinfo
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.result.Result
import com.preference.PowerPreference
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.serialization.ImplicitReflectionSerializer

@ImplicitReflectionSerializer
class LoginActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_login)

    limitContainerWidth()
  }

  override fun onResume() {
    super.onResume()

    anonymous?.setOnCheckedChangeListener { _, isChecked ->
      val state = when (isChecked) {
        true -> View.GONE
        false -> View.VISIBLE
      }

      username_field.visibility = state
      password_field.visibility = state
    }

    login?.setOnClickListener {
      var hostname = hostname.text.toString().trim()
      val username = username.text.toString()
      val password = password.text.toString()

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
          false -> authedLogin(hostname, username, password)
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

  private fun authedLogin(hostname: String, username: String, password: String) {
    val body = mapOf(
      "username" to username,
      "password" to password
    ).toList()

    val dialog = LoginDialog().apply {
      show(supportFragmentManager, "LoginDialog")
    }

    lifecycleScope.launch(Main) {
      try {
        val (_, response, result) =
          Fuel
            .post("$hostname/api/v1/token/", body)
            .awaitObjectResponseResult<Credentials>(AppContext.deserializer())

        when (result) {
          is Result.Success<*> -> {
            PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).apply {
              setString("hostname", hostname)
              setBoolean("anonymous", false)
              setString("username", username)
              setString("password", password)
              setString("access_token", result.get().token)
            }

            Userinfo.get()?.let {
              dialog.dismiss()
              startActivity(Intent(this@LoginActivity, MainActivity::class.java))

              return@launch finish()
            }

            throw Exception(getString(R.string.login_error_userinfo))
          }

          is Result.Failure -> {
            dialog.dismiss()

            val error = AppContext.json.parse(Credentials.serializer(), String(response.data))

            hostname_field.error = null
            username_field.error = null

            if (error.non_field_errors?.isNotEmpty() == true) {
              username_field.error = error.non_field_errors[0]
            } else {
              hostname_field.error = result.error.localizedMessage
            }
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

  private fun anonymousLogin(hostname: String) {
    val dialog = LoginDialog().apply {
      show(supportFragmentManager, "LoginDialog")
    }

    lifecycleScope.launch(Main) {
      try {
        val (_, _, result) = Fuel.get("$hostname/api/v1/tracks/")
          .awaitObjectResponseResult<Credentials>(AppContext.deserializer())

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