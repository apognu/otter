package com.github.apognu.otter.models.domain

import com.couchbase.lite.Result
import com.github.apognu.otter.models.dao.DecoratedAlbumEntity

data class Album(
  val id: Int,
  val title: String,
  val artist_id: Int,
  val cover: String? = null,
  val release_date: String? = null,
  var artist_name: String = ""
): SearchResult {

  companion object {
    fun from(album: Result) = album.getDictionary(0).run {
      Album(
        getInt("id"),
        getString("title") ?: "N/A",
        0,
        getString("cover"),
        getString("release_date"),
        getString("artist_name") ?: "N/A"
      )
    }

    fun fromDecoratedEntity(entity: DecoratedAlbumEntity): Album = entity.run {
      Album(
        id,
        title,
        artist_id,
        cover,
        release_date,
        artist_name
      )
    }
  }

  override fun cover() = cover
  override fun title() = title
  override fun subtitle() = artist_name
}