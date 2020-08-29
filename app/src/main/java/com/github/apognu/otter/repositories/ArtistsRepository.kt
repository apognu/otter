package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.utils.Artist
import com.github.apognu.otter.utils.ArtistsCache
import com.github.apognu.otter.utils.ArtistsResponse
import com.github.apognu.otter.utils.OtterResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class ArtistsRepository(override val context: Context?) : Repository<Artist, ArtistsCache>() {
  override val cacheId = "artists"
  override val upstream = HttpUpstream<Artist, OtterResponse<Artist>>(context, HttpUpstream.Behavior.Progressive, "/api/v1/artists/?playable=true&ordering=name", object : TypeToken<ArtistsResponse>() {}.type)

  override fun cache(data: List<Artist>) = ArtistsCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(ArtistsCache::class.java).deserialize(reader)
}