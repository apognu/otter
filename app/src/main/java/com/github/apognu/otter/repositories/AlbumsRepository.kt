package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.AlbumsCache
import com.github.apognu.otter.utils.AlbumsResponse
import com.github.apognu.otter.utils.OtterResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class AlbumsRepository(override val context: Context?, artistId: Int? = null) : Repository<Album, AlbumsCache>() {
  override val cacheId: String by lazy {
    if (artistId == null) "albums"
    else "albums-artist-$artistId"
  }

  override val upstream: Upstream<Album> by lazy {
    val url =
      if (artistId == null) "/api/v1/albums/?playable=true&ordering=title"
      else "/api/v1/albums/?playable=true&artist=$artistId&ordering=release_date"

    HttpUpstream<Album, OtterResponse<Album>>(
      context,
      HttpUpstream.Behavior.Progressive,
      url,
      object : TypeToken<AlbumsResponse>() {}.type
    )
  }

  override fun cache(data: List<Album>) = AlbumsCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(AlbumsCache::class.java).deserialize(reader)
}