package com.github.apognu.otter.models.domain

import com.github.apognu.otter.models.dao.DecoratedArtistEntity

data class Artist(
  val id: Int,
  val name: String,
  val album_count: Int = 0,
  val album_cover: String? = "",
  var albums: List<Album> = listOf()
) : SearchResult {

  companion object {
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