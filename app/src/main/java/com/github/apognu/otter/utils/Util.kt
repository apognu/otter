package com.github.apognu.otter.utils

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.util.Log
import com.preference.PowerPreference
import net.openid.appauth.AuthState
import java.net.URI

fun Context?.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
  if (this != null) {
    Toast.makeText(this, message, length).show()
  }
}

private fun logClassName(): String {
  val known = setOf(
    "dalvik.system.VMStack",
    "java.lang.Thread",
    "com.github.apognu.otter.utils.UtilKt"
  )

  Thread.currentThread().stackTrace.forEach {
    if (!known.contains(it.className)) {
      val className = it.className.split('.').last()
      val line = it.lineNumber

      return "$className:$line"
    }
  }

  return "UNKNOWN"
}

fun Any?.log(prefix: String? = null) {
  prefix?.let {
    Log.d("OTTER", "${logClassName()} - $prefix: $this")
  } ?: Log.d("OTTER", "${logClassName()} - $this")
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

  return uri.toString()
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
  fun hasAccessToken() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).contains("state") && OAuth.state().isAuthorized
  fun getAccessToken() = OAuth.state().accessToken
  fun isAnonymous() = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getBoolean("anonymous", false)
  fun areExperimentsEnabled() = PowerPreference.getDefaultFile().getBoolean("experiments", false)
  fun getScope() = PowerPreference.getDefaultFile().getString("scope", "all")
}
