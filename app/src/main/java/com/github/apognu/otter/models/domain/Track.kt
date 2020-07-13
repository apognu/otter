package com.github.apognu.otter.models.domain

import com.github.apognu.otter.models.dao.DecoratedTrackEntity
import com.preference.PowerPreference

data class Track(
  val id: Int,
  val title: String,
  val position: Int?,
  val copyright: String?,
  val license: String?,

  // Virtual attributes
  val favorite: Boolean,
  var current: Boolean = false,
  var cached: Boolean = false,
  var downloaded: Boolean = false,

  // Associations
  val artist: Artist? = null,
  val album: Album? = null,
  var uploads: List<Upload> = listOf()
) : SearchResult {

  companion object {
    fun fromDecoratedEntity(entity: DecoratedTrackEntity) = entity.run {
      Track(
        id,
        title,
        position,
        copyright,
        license,
        favorite,
        artist = entity.artist?.let { Artist.fromDecoratedEntity(it) },
        album = entity.album?.let { Album.fromDecoratedEntity(it) },
        uploads = uploads.map { Upload.fromEntity(it) }
      )
    }
  }

  override fun cover() = album?.cover
  override fun title() = title
  override fun subtitle() = album?.title ?: "N/A"

  fun bestUpload(): Upload? {
    if (uploads.isEmpty()) return null

    return when (PowerPreference.getDefaultFile().getString("media_cache_quality")) {
      "quality" -> uploads.maxBy { it.bitrate } ?: uploads[0]
      "size" -> uploads.minBy { it.bitrate } ?: uploads[0]
      else -> uploads.maxBy { it.bitrate } ?: uploads[0]
    }
  }
}