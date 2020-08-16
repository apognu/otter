package com.github.apognu.otter.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.github.kittinunf.fuel.gson.jsonBody
import com.github.kittinunf.result.Result
import com.preference.PowerPreference
import kotlinx.coroutines.runBlocking
import net.openid.appauth.*
import java.util.*

fun AuthState.save() {
  PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).apply {
    setString("state", jsonSerializeString())
  }
}

object OAuth {
  data class App(val client_id: String, val client_secret: String)

  val REDIRECT_URI = Uri.parse("urn:/com.github.apognu.otter/oauth/callback")

  fun state(): AuthState = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("state").run {
    AuthState.jsonDeserialize(this)
  }

  fun init(hostname: String) {
    AuthState(config(hostname)).save()
  }

  fun service(context: Context) = AuthorizationService(context)

  fun register(context: Context, callback: () -> Unit) {
    state().authorizationServiceConfiguration?.let { config ->
      val body = mapOf(
        "name" to UUID.randomUUID(),
        "redirect_uris" to REDIRECT_URI.toString()
      )

      runBlocking {
        val (_, _, result) = Fuel.post(config.registrationEndpoint.toString())
          .header("Content-Type", "application/json")
          .jsonBody(body)
          .awaitObjectResponseResult(gsonDeserializerOf(App::class.java))

        when (result) {
          is Result.Success -> {
            val app = result.get()

            val response = RegistrationResponse.Builder(registration()!!)
              .setClientId(app.client_id)
              .setClientSecret(app.client_secret)
              .setClientIdIssuedAt(0)
              .setClientSecretExpiresAt(null)
              .build()

            state().apply {
              update(response)
              save()

              callback()
            }
          }

          is Result.Failure -> {
          }
        }
      }
    }
  }

  fun authorize(context: Activity) {
    val intent = service(context).run {
      authorizationRequest()?.let {
        getAuthorizationRequestIntent(it)
      }
    }

    context.startActivityForResult(intent, 0)
  }

  fun exchange(context: Activity, authorization: Intent, success: () -> Unit, error: () -> Unit) {
    state().let { state ->
      state.apply {
        update(AuthorizationResponse.fromIntent(authorization), AuthorizationException.fromIntent(authorization))
        save()
      }

      AuthorizationResponse.fromIntent(authorization)?.let {
        val auth = ClientSecretPost(state().clientSecret)

        service(context).performTokenRequest(it.createTokenExchangeRequest(), auth) { response, e ->
          state
            .apply {
              update(response, e)
              save()
            }

          if (response != null) success()
          else error()
        }
      }
    }
  }

  private fun config(hostname: String) = AuthorizationServiceConfiguration(
    Uri.parse("$hostname/authorize"),
    Uri.parse("$hostname/api/v1/oauth/token/"),
    Uri.parse("$hostname/api/v1/oauth/apps/")
  )

  private fun registration() = state().authorizationServiceConfiguration?.let { config ->
    RegistrationRequest.Builder(config, listOf(REDIRECT_URI)).build()
  }

  private fun authorizationRequest() = state().let { state ->
    state.authorizationServiceConfiguration?.let { config ->
      AuthorizationRequest.Builder(
        config,
        state.lastRegistrationResponse?.clientId ?: "",
        ResponseTypeValues.CODE,
        REDIRECT_URI
      )
        .setScopes(
          /* "read:profile",
          "read:libraries",
          "write:libraries",
          "read:favorites",
          "write:favorites",
          "read:playlists",
          "write:playlists",
          "read:radios",
          "write:listenings" */
        "read", "write"
        )
        .build()
    }
  }
}