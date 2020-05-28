package com.github.apognu.otter.utils

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.util.Log
import com.preference.PowerPreference
import java.net.URI

fun Context?.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
  if (this != null) {
    Toast.makeText(this, message, length).show()
  }
}

fun Any.log(message: String) {
  Log.d("FUNKWHALE", "${this.javaClass.simpleName}: $message")
}

fun maybeNormalizeUrl(url: String?): String? {
  if (url == null || url.isEmpty()) return null

  val fallbackHost = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("hostname")
  val uri = URI(url).takeIf { it.host != null } ?: URI("$fallbackHost$url")

  return uri.run {
    URI("https", host, path, query, null)
  }.toString()
}

fun mustNormalizeUrl(url: String): String {
  val fallbackHost = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("hostname")
  val uri = URI(url).takeIf { it.host != null } ?: URI("$fallbackHost$url")

  return uri.run {
    URI("https", host, path, query, null)
  }.toString()
}

fun toDurationString(seconds: Long): String {
  val days = (seconds / 86400)
  val hours = (seconds % 86400) / 3600
  val minutes = (seconds % 86400 % 3600) / 60

  val ret = StringBuilder()

  if (days > 0) ret.append("${days}d")
  if (hours > 0) ret.append(" ${hours}h")
  if (minutes > 0) ret.append(" ${minutes}m")

  return ret.toString()
}

object Settings {
  fun hasAccessToken() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).contains("access_token")
  fun getAccessToken(): String = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token", "")
  fun isAnonymous() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getBoolean("anonymous", false)
}