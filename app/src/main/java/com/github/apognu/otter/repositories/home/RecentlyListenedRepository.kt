package com.github.apognu.otter.repositories.home

import android.content.Context
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.*
import com.google.gson.reflect.TypeToken

class RecentlyListenedRepository(override val context: Context?) : Repository<PlaylistTrack, PlaylistTracksCache>() {
  override val cacheId = "home-recently-listened"

  override val upstream =
    HttpUpstream<PlaylistTrack, FunkwhaleResponse<PlaylistTrack>>(
      HttpUpstream.Behavior.Single,
      "/api/v1/history/listenings/?playable=true",
      object : TypeToken<PlaylistTracksResponse>() {}.type,
      10
    )
}