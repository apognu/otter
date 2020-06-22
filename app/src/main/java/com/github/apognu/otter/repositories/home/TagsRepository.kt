package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.FunkwhaleResponse
import com.github.apognu.otter.utils.Tag
import com.github.apognu.otter.utils.TagsCache
import com.github.apognu.otter.utils.TagsResponse
import com.github.kittinunf.fuel.gson.gsonDeserializerOf
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader

class TagsRepository(override val context: Context?) : Repository<Tag, TagsCache>() {
  override val cacheId = "home-tags"

  override val upstream =
    HttpUpstream<Tag, FunkwhaleResponse<Tag>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/tags/",
      object : TypeToken<TagsResponse>() {}.type
    )

  override fun onDataFetched(data: List<Tag>) = data.shuffled().take(10)

  override fun cache(data: List<Tag>) = TagsCache(data)
  override fun uncache(reader: BufferedReader) = gsonDeserializerOf(TagsCache::class.java).deserialize(reader)
}