package com.github.apognu.otter.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.LoginDialog
import com.github.apognu.otter.utils.AppContext
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.preference.PowerPreference
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class FwCredentials(val token: String)

class LoginActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_login)
  }

  override fun onResume() {
    super.onResume()

    login?.setOnClickListener {
      var hostname = hostname.text.toString().trim()
      val username = username.text.toString()
      val password = password.text.toString()

      try {
        if (hostname.isEmpty()) throw Exception(getString(R.string.login_error_hostname))

        Uri.parse(hostname).apply {
          if (scheme == "http") {
            throw Exception(getString(R.string.login_error_hostname_https))
          }

          if (scheme == null) hostname = "https://${hostname}"
        }
      } catch (e: Exception) {
        val message =
          if (e.message?.isEmpty() == true) getString(R.string.login_error_hostname)
          else e.message

        hostname_field.error = message
      }

      hostname_field.error = ""

      val body = mapOf(
        "username" to username,
        "password" to password
      ).toList()

      val dialog = LoginDialog().apply {
        show(supportFragmentManager, "LoginDialog")
      }

      GlobalScope.launch(Main) {
        try {
          val result = Fuel.post("$hostname/api/v1/token/", body)
            .awaitObjectResult(gsonDeserializerOf(FwCredentials::class.java))

          result.fold(
            { data ->
              PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).apply {
                setString("hostname", hostname)
                setString("username", username)
                setString("password", password)
                setString("access_token", data.token)
              }

              dialog.dismiss()
              startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            },
            { error ->
              dialog.dismiss()

              hostname_field.error = error.localizedMessage
            }
          )
        } catch (e: Exception) {
          dialog.dismiss()

          val message =
            if (e.message?.isEmpty() == true) getString(R.string.login_error_hostname)
            else e.message

          hostname_field.error = message
        }
      }
    }
  }
}