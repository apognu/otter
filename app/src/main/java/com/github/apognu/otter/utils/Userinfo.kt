package com.github.apognu.otter.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.github.kittinunf.result.Result
import com.preference.PowerPreference

object Userinfo {
  suspend fun get(): User? {
    try {
      val hostname = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("hostname")
      val (_, _, result) = Fuel.get("$hostname/api/v1/users/users/me/")
        .authorize()
        .awaitObjectResponseResult(gsonDeserializerOf(User::class.java))

      return when (result) {
        is Result.Success -> {
          val user = result.get()

          PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).apply {
            setString("actor_username", user.full_username)
          }

          user
        }

        else -> null
      }
    } catch (e: Exception) {
      return null
    }
  }
}