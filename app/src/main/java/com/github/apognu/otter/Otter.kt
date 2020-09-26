package com.github.apognu.otter

import android.app.Application
import android.content.Context
import android.widget.SearchView
import androidx.appcompat.app.AppCompatDelegate
import androidx.room.Room
import com.couchbase.lite.*
import com.github.apognu.otter.activities.MainActivity
import com.github.apognu.otter.activities.SearchActivity
import com.github.apognu.otter.adapters.*
import com.github.apognu.otter.fragments.*
import com.github.apognu.otter.models.Mediator
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.playback.MediaSession
import com.github.apognu.otter.playback.QueueManager.Companion.factory
import com.github.apognu.otter.repositories.*
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.viewmodels.*
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.DefaultDownloaderFactory
import com.google.android.exoplayer2.offline.DownloadManager
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.preference.PowerPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.fragment.dsl.fragment
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class Otter : Application() {
  companion object {
    private var instance: Otter = Otter()

    fun get(): Otter = instance
  }

  var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

  val eventBus: BroadcastChannel<Event> = BroadcastChannel(10)
  val commandBus: BroadcastChannel<Command> = BroadcastChannel(10)

  private val exoDatabase: ExoDatabaseProvider by lazy { ExoDatabaseProvider(this) }

  val exoCache: SimpleCache by lazy {
    PowerPreference.getDefaultFile().getInt("media_cache_size", 1).toLong().let {
      val cacheSize = if (it == 0L) 0 else it * 1024 * 1024 * 1024

      SimpleCache(
        cacheDir.resolve("media"),
        LeastRecentlyUsedCacheEvictor(cacheSize),
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
    DownloaderConstructorHelper(exoDownloadCache, factory(this)).run {
      DownloadManager(this@Otter, DefaultDownloadIndex(exoDatabase), DefaultDownloaderFactory(this))
    }
  }

  val mediaSession = MediaSession(this)

  override fun onCreate() {
    super.onCreate()

    CouchbaseLite.init(this)

    defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler(CrashReportHandler())

    instance = this

    startKoin {
      androidLogger()
      androidContext(this@Otter)

      modules(module {
        single {
          synchronized(this) {
            Room
              .databaseBuilder(get(), OtterDatabase::class.java, "otter")
              .build()
          }
        }

        single {
          Database("otter", DatabaseConfiguration()).apply {
            createIndex("type", IndexBuilder.valueIndex(ValueIndexItem.expression(Expression.property("type"))))
            createIndex("order", IndexBuilder.valueIndex(ValueIndexItem.expression(Expression.property("order"))))
          }
        }

        fragment { BrowseFragment() }
        fragment { LandscapeQueueFragment() }

        single { PlayerStateViewModel(get()) }

        single { ArtistsRepository(get(), get()) }
        factory { (id: Int) -> ArtistTracksRepository(get(), get(), id) }
        viewModel { ArtistsViewModel(get()) }
        factory { (context: Context?, listener: ArtistsFragment.OnArtistClickListener) -> ArtistsAdapter(context, listener) }

        factory { (id: Int?) -> AlbumsRepository(get(), get(), id) }
        viewModel { (id: Int?) -> AlbumsViewModel(get { parametersOf(id) }, get { parametersOf(id) }, id) }
        factory { (context: Context?, adapter: AlbumsAdapter.OnAlbumClickListener) -> AlbumsAdapter(context, adapter) }
        factory { (context: Context?, adapter: AlbumsGridAdapter.OnAlbumClickListener) -> AlbumsGridAdapter(context, adapter) }

        factory { (id: Int?) -> TracksRepository(get(), get(), get(), id) }
        viewModel { (id: Int) -> TracksViewModel(get { parametersOf(id) }, get(), id) }
        factory { (context: Context?, favoriteListener: TracksAdapter.OnFavoriteListener?) -> TracksAdapter(context, favoriteListener) }

        single { PlaylistsRepository(get(), get()) }
        factory { (id: Int) -> PlaylistTracksRepository(get(), get(), id) }
        viewModel { PlaylistsViewModel(get()) }
        viewModel { (id: Int) -> PlaylistViewModel(get { parametersOf(id) }, get { parametersOf(null) }, get(), id) }
        factory { (context: Context?, listener: PlaylistsAdapter.OnPlaylistClickListener) -> PlaylistsAdapter(context, listener) }
        factory { (context: Context?, listener: PlaylistTracksAdapter.OnFavoriteListener) -> PlaylistTracksAdapter(context, listener) }

        single { FavoritesRepository(get(), get()) }
        single { FavoritedRepository(get(), get()) }
        factory { (context: Context?, listener: FavoritesAdapter.OnFavoriteListener) -> FavoritesAdapter(context, listener) }
        viewModel { FavoritesViewModel(get(), get { parametersOf(null) }) }

        single { RadiosRepository(get(), get()) }
        factory { (context: Context?, scope: CoroutineScope, listener: RadiosAdapter.OnRadioClickListener) -> RadiosAdapter(context, scope, listener) }
        viewModel { RadiosViewModel(get()) }

        single { (scope: CoroutineScope) -> QueueRepository(get(), scope) }
        viewModel { QueueViewModel(get(), get()) }

        viewModel { SearchViewModel(get(), get(), get()) }
        single { ArtistsSearchRepository(get(), get()) }
        single { AlbumsSearchRepository(get(), get { parametersOf(null) }) }
        single { TracksSearchRepository(get(), get { parametersOf(null) }) }
      })
    }

    when (PowerPreference.getDefaultFile().getString("night_mode")) {
      "on" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      "off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
      else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
  }

  fun deleteAllData() {
    PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).clear()

    filesDir.deleteRecursively()

    cacheDir.listFiles()?.forEach {
      it.delete()
    }

    cacheDir.resolve("picasso-cache").deleteRecursively()
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