package com.github.apognu.otter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.github.apognu.otter.playback.QueueManager
import com.github.apognu.otter.utils.*
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.preference.PowerPreference
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import java.text.SimpleDateFormat
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

  private val exoDatabase: ExoDatabaseProvider by lazy { ExoDatabaseProvider(this) }

  val exoCache: SimpleCache by lazy {
    PowerPreference.getDefaultFile().getInt("media_cache_size", 1).toLong().let {
      SimpleCache(
        cacheDir.resolve("media"),
        LeastRecentlyUsedCacheEvictor(it * 1024 * 1024 * 1024),
        exoDatabase
      )
    }
  }

  val exoDownloadCache: SimpleCache by lazy {
    SimpleCache(
      cacheDir.resolve("downloads"),
      NoOpCacheEvictor(),
      exoDatabase
    )
  }

  val exoDownloadManager: DownloadManager by lazy {
    DownloaderConstructorHelper(exoDownloadCache, QueueManager.factory(this)).run {
      DownloadManager(this@Otter, DefaultDownloadIndex(exoDatabase), DefaultDownloaderFactory(this))
    }
  }

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