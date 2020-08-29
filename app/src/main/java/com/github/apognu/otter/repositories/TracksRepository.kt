package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.android.exoplayer2.offline.Download
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class TracksRepository(override val context: Context?, albumId: Int) : Repository<Track, TracksCache>() {
  override val cacheId = "tracks-album-$albumId"
  override val upstream = HttpUpstream<Track, OtterResponse<Track>>(context, HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&album=$albumId&ordering=disc_number,position", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  companion object {
    fun getDownloadedIds(): List<Int>? {
      val cursor = Otter.get().exoDownloadManager.downloadIndex.getDownloads()
      val ids: MutableList<Int> = mutableListOf()

      while (cursor.moveToNext()) {
        val download = cursor.download

        download.getMetadata()?.let {
          if (download.state == Download.STATE_COMPLETED) {
            ids.add(it.id)
          }
        }
      }

      return ids
    }
  }

  override fun onDataFetched(data: List<Track>): List<Track> = runBlocking {
    val favorites = FavoritedRepository(context).fetch(Origin.Cache.origin)
      .map { it.data }
      .toList()
      .flatten()

    val downloaded = getDownloadedIds() ?: listOf()

    data.map { track ->
      track.favorite = favorites.contains(track.id)
      track.downloaded = downloaded.contains(track.id)

      track.bestUpload()?.let { upload ->
        val url = mustNormalizeUrl(upload.listen_url)

        track.cached = Otter.get().exoCache.isCached(url, 0, upload.duration * 1000L)
      }

      track
    }.sortedWith(compareBy({ it.disc_number }, { it.position }))
  }
}