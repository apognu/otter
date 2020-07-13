package com.github.apognu.otter.models.api

import com.github.apognu.otter.models.domain.SearchResult
import com.google.android.exoplayer2.offline.Download
import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleTrack(
  val id: Int = 0,
  val title: String,
  val artist: FunkwhaleArtist,
  val album: FunkwhaleAlbum?,
  val disc_number: Int? = null,
  val position: Int = 0,
  val uploads: List<FunkwhaleUpload> = listOf(),
  val copyright: String? = null,
  val license: String? = null
) : SearchResult {
  var current: Boolean = false
  var favorite: Boolean = false
  var cached: Boolean = false
  var downloaded: Boolean = false

  companion object {
    fun fromDownload(download: DownloadInfo): FunkwhaleTrack = FunkwhaleTrack(
      id = download.id,
      title = download.title,
      artist = FunkwhaleArtist(0, download.artist, listOf()),
      album = FunkwhaleAlbum(0, FunkwhaleAlbum.Artist(0, ""), "", Covers(CoverUrls("")), ""),
      uploads = listOf(FunkwhaleUpload(download.contentId, 0, 0))
    )
  }

  @Serializable
  data class FunkwhaleUpload(
    val listen_url: String,
    val duration: Int,
    val bitrate: Int
  )

  override fun hashCode() = id

  override fun equals(other: Any?): Boolean {
    return when (other) {
      is FunkwhaleTrack -> other.id == id
      else -> false
    }
  }

  override fun cover() = album?.cover()
  override fun title() = title
  override fun subtitle() = artist.name
}

@Serializable
data class Favorited(val track: Int)

data class DownloadInfo(
  val id: Int,
  val contentId: String,
  val title: String,
  val artist: String,
  var download: Download?
)