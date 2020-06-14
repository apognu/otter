package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.*
import com.google.gson.reflect.TypeToken

class RecentlyAddedRepository(override val context: Context?) : Repository<Track, TracksCache>() {
  override val cacheId = "home-recently-added"

  override val upstream =
    HttpUpstream<Track, FunkwhaleResponse<Track>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/tracks/?playable=true&ordering=-creation_date",
      object : TypeToken<TracksResponse>() {}.type,
      10
    )
}