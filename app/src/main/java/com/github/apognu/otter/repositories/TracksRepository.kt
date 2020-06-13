package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.*
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.android.exoplayer2.offline.Download
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader

class TracksRepository(override val context: Context?, albumId: Int) : Repository<Track, TracksCache>() {
  override val cacheId = "tracks-album-$albumId"
  override val upstream = HttpUpstream<Track, FunkwhaleResponse<Track>>(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&album=$albumId", object : TypeToken<TracksResponse>() {}.type)

  override fun cache(data: List<Track>) = TracksCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TracksCache::class.java).deserialize(reader)

  companion object {
    suspend fun getDownloadedIds(): List<Int>? {
      return RequestBus.send(Request.GetDownloads).wait<com.github.apognu.otter.utils.Response.Downloads>()?.let { response ->
        val ids: MutableList<Int> = mutableListOf()

        while (response.cursor.moveToNext()) {
          val download = response.cursor.download

          Gson().fromJson(String(download.request.data), DownloadInfo::class.java)?.let {
            if (download.state == Download.STATE_COMPLETED) {
              ids.add(it.id)
            }
          }
        }

        ids
      }
    }
  }

  override fun onDataFetched(data: List<Track>): List<Track> = runBlocking {
    val favorites = FavoritedRepository(context).fetch(Origin.Network.origin)
      .map { it.data }
      .toList()
      .flatten()

    val downloaded = getDownloadedIds() ?: listOf()

    data.map { track ->
      track.favorite = favorites.contains(track.id)
      track.downloaded = downloaded.contains(track.id)
      track
    }.sortedBy { it.position }
  }
}