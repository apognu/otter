package com.github.apognu.otter.utils

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.util.Log
import com.preference.PowerPreference
import java.net.URI
import java.net.URL

fun Context?.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
  if (this != null) {
    Toast.makeText(this, message, length).show()
  }
}

fun Any.log(message: String) {
  Log.d("FUNKWHALE", "${this.javaClass.simpleName}: $message")
}

fun Any.log() {
  Log.d("FUNKWHALE", this.toString())
}

fun maybeNormalizeUrl(rawUrl: String?): String? {
  try {
    if (rawUrl == null || rawUrl.isEmpty()) return null

    return mustNormalizeUrl(rawUrl)
  } catch (e: Exception) {
    return null
  }
}

fun mustNormalizeUrl(rawUrl: String): String {
  val fallbackHost = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("hostname")
  val uri = URI(rawUrl).takeIf { it.host != null } ?: URI("$fallbackHost$rawUrl")

  return uri.toURL().run {
    URL("https", host, file)
  }.toString()
}

fun toDurationString(duration: Long, showSeconds: Boolean = false): String {
  val days = (duration / 86400)
  val hours = (duration % 86400) / 3600
  val minutes = (duration % 86400 % 3600) / 60
  val seconds = duration % 86400 % 3600 % 60

  val ret = StringBuilder()

  if (days > 0) ret.append("${days}d ")
  if (hours > 0) ret.append("${hours}h ")
  if (minutes > 0) ret.append("${minutes}m ")
  if (showSeconds && seconds > 0) ret.append("${seconds}s")

  return ret.toString()
}

object Settings {
  fun hasAccessToken() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).contains("access_token")
  fun getAccessToken(): String = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token", "")
  fun isAnonymous() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getBoolean("anonymous", false)
  fun areExperimentsEnabled() = PowerPreference.getDefaultFile().getBoolean("experiments", false)
  fun getScope() = PowerPreference.getDefaultFile().getString("scope", "all")
}
