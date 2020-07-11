package com.github.apognu.otter.utils

import com.google.android.exoplayer2.offline.Download
import com.preference.PowerPreference

data class User(
  val full_username: String
)

sealed class CacheItem<D : Any>(val data: List<D>)
class ArtistsCache(data: List<Artist>) : CacheItem<Artist>(data)
class AlbumsCache(data: List<Album>) : CacheItem<Album>(data)
class TracksCache(data: List<Track>) : CacheItem<Track>(data)
class PlaylistsCache(data: List<Playlist>) : CacheItem<Playlist>(data)
class PlaylistTracksCache(data: List<PlaylistTrack>) : CacheItem<PlaylistTrack>(data)
class RadiosCache(data: List<Radio>) : CacheItem<Radio>(data)
class FavoritedCache(data: List<Int>) : CacheItem<Int>(data)
class QueueCache(data: List<Track>) : CacheItem<Track>(data)

abstract class OtterResponse<D : Any> {
  abstract val count: Int
  abstract val next: String?

  abstract fun getData(): List<D>
}

data class UserResponse(override val count: Int, override val next: String?, val results: List<Artist>) : OtterResponse<Artist>() {
  override fun getData() = results
}

data class ArtistsResponse(override val count: Int, override val next: String?, val results: List<Artist>) : OtterResponse<Artist>() {
  override fun getData() = results
}

data class AlbumsResponse(override val count: Int, override val next: String?, val results: AlbumList) : OtterResponse<Album>() {
  override fun getData() = results
}

data class TracksResponse(override val count: Int, override val next: String?, val results: List<Track>) : OtterResponse<Track>() {
  override fun getData() = results
}

data class FavoritedResponse(override val count: Int, override val next: String?, val results: List<Favorited>) : OtterResponse<Int>() {
  override fun getData() = results.map { it.track }
}

data class PlaylistsResponse(override val count: Int, override val next: String?, val results: List<Playlist>) : OtterResponse<Playlist>() {
  override fun getData() = results
}

data class PlaylistTracksResponse(override val count: Int, override val next: String?, val results: List<PlaylistTrack>) : OtterResponse<PlaylistTrack>() {
  override fun getData() = results
}

data class RadiosResponse(override val count: Int, override val next: String?, val results: List<Radio>) : OtterResponse<Radio>() {
  override fun getData() = results
}

data class Covers(val original: String)

typealias AlbumList = List<Album>

interface SearchResult {
  fun cover(): String?
  fun title(): String
  fun subtitle(): String
}

data class Album(
  val id: Int,
  val artist: Artist,
  val title: String,
  val cover: Covers,
  val release_date: String?
) : SearchResult {
  data class Artist(val name: String)

  override fun cover() = cover.original
  override fun title() = title
  override fun subtitle() = artist.name
}

data class Artist(
  val id: Int,
  val name: String,
  val albums: List<Album>?
) : SearchResult {
  data class Album(
    val title: String,
    val cover: Covers
  )

  override fun cover() = albums?.getOrNull(0)?.cover?.original
  override fun title() = name
  override fun subtitle() = "Artist"
}

data class Track(
  val id: Int = 0,
  val title: String,
  val artist: Artist,
  val album: Album?,
  val position: Int = 0,
  val uploads: List<Upload> = listOf(),
  val copyright: String? = null,
  val license: String? = null
) : SearchResult {
  var current: Boolean = false
  var favorite: Boolean = false
  var cached: Boolean = false
  var downloaded: Boolean = false

  companion object {
    fun fromDownload(download: DownloadInfo): Track = Track(
      id = download.id,
      title = download.title,
      artist = Artist(0, download.artist, listOf()),
      album = Album(0, Album.Artist(""), "", Covers(""), ""),
      uploads = listOf(Upload(download.contentId, 0, 0))
    )
  }

  data class Upload(
    val listen_url: String,
    val duration: Int,
    val bitrate: Int
  )

  override fun equals(other: Any?): Boolean {
    return when (other) {
      is Track -> other.id == id
      else -> false
    }
  }

  fun bestUpload(): Upload? {
    if (uploads.isEmpty()) return null

    return when (PowerPreference.getDefaultFile().getString("media_cache_quality")) {
      "quality" -> uploads.maxBy { it.bitrate } ?: uploads[0]
      "size" -> uploads.minBy { it.bitrate } ?: uploads[0]
      else -> uploads.maxBy { it.bitrate } ?: uploads[0]
    }
  }

  override fun cover() = album?.cover?.original
  override fun title() = title
  override fun subtitle() = artist.name
}

data class Favorited(val track: Int)

data class Playlist(
  val id: Int,
  val name: String,
  val album_covers: List<String>,
  val tracks_count: Int,
  val duration: Int
)

data class PlaylistTrack(val track: Track)

data class Radio(
  val id: Int,
  var radio_type: String,
  val name: String,
  val description: String,
  var related_object_id: String? = null
)

data class DownloadInfo(
  val id: Int,
  val contentId: String,
  val title: String,
  val artist: String,
  var download: Download?
)