package com.github.apognu.otter.models.domain

import com.couchbase.lite.Result
import com.github.apognu.otter.models.dao.DecoratedArtistEntity

data class Artist(
  val id: Int,
  val name: String,
  val album_count: Int = 0,
  val album_cover: String? = "",
  var albums: List<Album> = listOf()
) : SearchResult {

  companion object {
    fun from(artist: Result) = artist.getDictionary(0).run {
      Artist(
        getInt("id"),
        getString("name") ?: "N/A",
        getArray("albums")?.count() ?: 0,
        getString("cover")
      )
    }

    fun fromDecoratedEntity(entity: DecoratedArtistEntity): Artist = entity.run {
      Artist(
        id,
        name,
        album_count = album_count,
        album_cover = album_cover
      )
    }
  }

  override fun cover() = album_cover
  override fun title() = name
  override fun subtitle() = "Artist"
}