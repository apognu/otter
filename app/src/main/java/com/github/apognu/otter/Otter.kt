package com.github.apognu.otter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.github.apognu.otter.utils.*
import com.preference.PowerPreference
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

class Otter : Application() {
  companion object {
    private var instance: Otter = Otter()

    fun get(): Otter = instance
  }

  var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

  val eventBus: BroadcastChannel<Event> = BroadcastChannel(10)
  val commandBus: Channel<Command> = Channel(10)
  val requestBus: BroadcastChannel<Request> = BroadcastChannel(10)
  val progressBus: BroadcastChannel<Triple<Int, Int, Int>> = ConflatedBroadcastChannel()

  override fun onCreate() {
    super.onCreate()

    defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler(CrashReportHandler())

    instance = this

    when (PowerPreference.getDefaultFile().getString("night_mode")) {
      "on" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      "off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
  }

  inner class CrashReportHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
      val now = Date(Date().time - (5 * 60 * 1000))
      val formatter = SimpleDateFormat("MM-dd kk:mm:ss.000", Locale.US)

      Runtime.getRuntime().exec(listOf("logcat", "-d", "-T", formatter.format(now)).toTypedArray()).also {
        it.inputStream.bufferedReader().also { reader ->
          val builder = StringBuilder()

          while (true) {
            builder.appendln(reader.readLine() ?: break)
          }

          builder.appendln(e.toString())

          Cache.set(this@Otter, "crashdump", builder.toString().toByteArray())
        }
      }

      defaultExceptionHandler?.uncaughtException(t, e)
    }
  }
}