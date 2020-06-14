package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.*
import com.google.gson.reflect.TypeToken

class TagsRepository(override val context: Context?) : Repository<Tag, TagsCache>() {
  override val cacheId = "tags"

  override val upstream =
    HttpUpstream<Tag, FunkwhaleResponse<Tag>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/tags/",
      object : TypeToken<TagsResponse>() {}.type
    )
}