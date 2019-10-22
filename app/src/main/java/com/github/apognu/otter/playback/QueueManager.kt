package com.github.apognu.otter.playback

import android.content.Context
import android.net.Uri
import com.github.apognu.otter.R
import com.github.apognu.otter.repositories.FavoritesRepository
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import com.preference.PowerPreference

class QueueManager(val context: Context) {
  var cache: SimpleCache
  var metadata: MutableList<Track> = mutableListOf()
  val datasources = ConcatenatingMediaSource()
  var current = -1

  init {
    PowerPreference.getDefaultFile().getInt("media_cache_size", 1).toLong().also {
      cache = SimpleCache(
        context.cacheDir.resolve("media"),
        LeastRecentlyUsedCacheEvictor(it * 1024 * 1024 * 1024)
      )
    }

    Cache.get(context, "queue")?.let { json ->
      gsonDeserializerOf(QueueCache::class.java).deserialize(json)?.let { cache ->
        metadata = cache.data.toMutableList()

        val factory = factory()

        datasources.addMediaSources(metadata.map { track ->
          val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

          ProgressiveMediaSource.Factory(factory).setTag(track.title).createMediaSource(Uri.parse(url))
        })
      }
    }

    Cache.get(context, "current")?.let { string ->
      current = string.readLine().toInt()
    }
  }

  private fun persist() {
    Cache.set(
      context,
      "queue",
      Gson().toJson(QueueCache(metadata)).toByteArray()
    )
  }

  private fun factory(): CacheDataSourceFactory {
    val token = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("access_token")

    val http = DefaultHttpDataSourceFactory(Util.getUserAgent(context, context.getString(R.string.app_name))).apply {
      defaultRequestProperties.apply {
        set("Authorization", "Bearer $token")
      }
    }

    return CacheDataSourceFactory(cache, http)
  }

  fun replace(tracks: List<Track>) {
    val factory = factory()

    val sources = tracks.map { track ->
      val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

      ProgressiveMediaSource.Factory(factory).setTag(track.title).createMediaSource(Uri.parse(url))
    }

    metadata = tracks.toMutableList()
    datasources.clear()
    datasources.addMediaSources(sources)

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun append(tracks: List<Track>) {
    val factory = factory()
    val tracks = tracks.filter { metadata.indexOf(it) == -1 }

    val sources = tracks.map { track ->
      val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

      ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url))
    }

    metadata.addAll(tracks)
    datasources.addMediaSources(sources)

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun insertNext(track: Track) {
    val factory = factory()
    val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

    if (metadata.indexOf(track) == -1) {
      ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url)).let {
        datasources.addMediaSource(current + 1, it)
        metadata.add(current + 1, track)
      }
    } else {
      move(metadata.indexOf(track), current + 1)
    }

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun remove(track: Track) {
    metadata.indexOf(track).let {
      datasources.removeMediaSource(it)
      metadata.removeAt(it)
    }

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun move(oldPosition: Int, newPosition: Int) {
    datasources.moveMediaSource(oldPosition, newPosition)
    metadata.add(newPosition, metadata.removeAt(oldPosition))

    persist()
  }

  fun get() = metadata.mapIndexed { index, track ->
    track.current = index == current
    track
  }

  fun get(index: Int): Track = metadata[index]

  fun current(): Track? {
    if (current == -1) {
      return metadata.getOrNull(0)
    }

    return metadata.getOrNull(current)
  }
}