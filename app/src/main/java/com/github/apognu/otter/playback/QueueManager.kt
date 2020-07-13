package com.github.apognu.otter.playback

import android.content.Context
import android.net.Uri
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.models.domain.Track
import com.github.apognu.otter.repositories.QueueRepository
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.viewmodels.PlayerStateViewModel
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class QueueManager(val context: Context) {
  private val queueRepository = QueueRepository(GlobalScope)

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
    GlobalScope.launch(IO) {
      queueRepository.allBlocking().also {
        replace(it.map { Track.fromDecoratedEntity(it) })
      }
    }

    Cache.get(context, "current")?.let { string ->
      current = string.readLine().toInt()

      PlayerStateViewModel.get()._track.postValue(current())
    }
  }

  private fun persist() = queueRepository.replace(metadata)

  fun replace(tracks: List<Track>) {
    metadata = tracks.toMutableList()

    val factory = factory(context)

    val sources = tracks.map { track ->
      val url = mustNormalizeUrl(track.bestUpload()?.listen_url ?: "")

      ProgressiveMediaSource.Factory(factory).setTag(track.title).createMediaSource(Uri.parse(url))
    }

    datasources.clear()
    datasources.addMediaSources(sources)

    persist()
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

    persist()
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

    persist()
  }

  fun remove(track: Track) {
    metadata.indexOf(track).let {
      if (it < 0) {
        return
      }

      datasources.removeMediaSource(it)
      metadata.removeAt(it)

      if (it == current) {
        CommandBus.send(Command.NextTrack)
      }

      if (it < current) {
        current--
      }
    }

    if (metadata.isEmpty()) {
      current = -1
    }

    persist()
  }

  fun move(oldPosition: Int, newPosition: Int) {
    datasources.moveMediaSource(oldPosition, newPosition)
    metadata.add(newPosition, metadata.removeAt(oldPosition))

    persist()
  }

  fun get() = metadata

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

    persist()
  }

  fun shuffle() {
    if (metadata.size < 2) return

    if (current == -1) {
      replace(metadata.shuffled())
    } else {
      move(current, 0)
      current = 0

      val shuffled =
        metadata
          .drop(1)
          .shuffled()

      while (metadata.size > 1) {
        datasources.removeMediaSource(metadata.size - 1)
        metadata.removeAt(metadata.size - 1)
      }

      append(shuffled)
    }

    persist()
  }
}