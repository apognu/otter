package com.github.apognu.otter.playback

import android.content.Context
import android.net.Uri
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson

class QueueManager(val context: Context, val cast: CastInterface?) {
  var metadata: MutableList<Track> = mutableListOf()
  val datasources = ConcatenatingMediaSource()
  var current = -1

  companion object {
    fun factory(context: Context): CacheDataSourceFactory {
      val http = DefaultHttpDataSourceFactory(Util.getUserAgent(context, context.getString(R.string.app_name))).apply {
        defaultRequestProperties.apply {
          if (!Settings.isAnonymous()) {
            set("Authorization", "Bearer ${Settings.getAccessToken()}")
          }
        }
      }

      val playbackCache = CacheDataSourceFactory(Otter.get().exoCache, http)

      return CacheDataSourceFactory(
        Otter.get().exoDownloadCache,
        playbackCache,
        FileDataSource.Factory(),
        null,
        CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
        null
      )
    }
  }

  init {
    Cache.get(context, "queue")?.let { json ->
      gsonDeserializerOf(QueueCache::class.java).deserialize(json)?.let { cache ->
        metadata = cache.data.toMutableList()

        val factory = factory(context)

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

  fun replace(tracks: List<Track>) {
    val factory = factory(context)

    val sources = tracks.map { track ->
      val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

      ProgressiveMediaSource.Factory(factory).setTag(track.title).createMediaSource(Uri.parse(url))
    }

    metadata = tracks.toMutableList()
    datasources.clear()
    datasources.addMediaSources(sources)

    cast?.replaceQueue(tracks)

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun append(tracks: List<Track>) {
    val factory = factory(context)
    val missingTracks = tracks.filter { metadata.indexOf(it) == -1 }

    val sources = missingTracks.map { track ->
      val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

      ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url))
    }

    metadata.addAll(tracks)
    datasources.addMediaSources(sources)

    cast?.addToQueue(tracks)

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun insertNext(track: Track) {
    val factory = factory(context)
    val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

    if (metadata.indexOf(track) == -1) {
      ProgressiveMediaSource.Factory(factory).createMediaSource(Uri.parse(url)).let {
        datasources.addMediaSource(current + 1, it)
        metadata.add(current + 1, track)
      }
    } else {
      move(metadata.indexOf(track), current + 1)
    }

    cast?.insertNext(track, current)

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun remove(track: Track) {
    metadata.indexOf(track).let { trackIndex ->
      if (trackIndex < 0) {
        return
      }

      datasources.removeMediaSource(trackIndex)
      metadata.removeAt(trackIndex)

      cast?.remove(trackIndex)

      if (trackIndex == current) {
        CommandBus.send(Command.NextTrack)
      }

      if (trackIndex < current) {
        current--
      }
    }

    if (metadata.isEmpty()) {
      current = -1
    }

    persist()

    EventBus.send(Event.QueueChanged)
  }

  fun move(oldPosition: Int, newPosition: Int) {
    datasources.moveMediaSource(oldPosition, newPosition)
    metadata.add(newPosition, metadata.removeAt(oldPosition))

    cast?.move(oldPosition, newPosition)

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

  fun clear() {
    metadata = mutableListOf()
    datasources.clear()
    current = -1
  }
}