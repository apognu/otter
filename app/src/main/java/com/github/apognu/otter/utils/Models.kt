package com.github.apognu.otter.utils

import com.preference.PowerPreference

sealed class CacheItem<D : Any>(val data: List<D>)
class ArtistsCache(data: List<Artist>) : CacheItem<Artist>(data)
class AlbumsCache(data: List<Album>) : CacheItem<Album>(data)
class TracksCache(data: List<Track>) : CacheItem<Track>(data)
class PlaylistsCache(data: List<Playlist>) : CacheItem<Playlist>(data)
class PlaylistTracksCache(data: List<PlaylistTrack>) : CacheItem<PlaylistTrack>(data)
class RadiosCache(data: List<Radio>) : CacheItem<Radio>(data)
class FavoritedCache(data: List<Int>) : CacheItem<Int>(data)
class QueueCache(data: List<Track>) : CacheItem<Track>(data)

abstract class FunkwhaleResponse<D : Any> {
  abstract val count: Int
  abstract val next: String?

  abstract fun getData(): List<D>
}

data class UserResponse(override val count: Int, override val next: String?, val results: List<Artist>) : FunkwhaleResponse<Artist>() {
  override fun getData() = results
}

data class ArtistsResponse(override val count: Int, override val next: String?, val results: List<Artist>) : FunkwhaleResponse<Artist>() {
  override fun getData() = results
}

data class AlbumsResponse(override val count: Int, override val next: String?, val results: AlbumList) : FunkwhaleResponse<Album>() {
  override fun getData() = results
}

data class TracksResponse(override val count: Int, override val next: String?, val results: List<Track>) : FunkwhaleResponse<Track>() {
  override fun getData() = results
}

data class FavoritedResponse(override val count: Int, override val next: String?, val results: List<Favorited>) : FunkwhaleResponse<Int>() {
  override fun getData() = results.map { it.track }
}

data class PlaylistsResponse(override val count: Int, override val next: String?, val results: List<Playlist>) : FunkwhaleResponse<Playlist>() {
  override fun getData() = results
}

data class PlaylistTracksResponse(override val count: Int, override val next: String?, val results: List<PlaylistTrack>) : FunkwhaleResponse<PlaylistTrack>() {
  override fun getData() = results
}

data class RadiosResponse(override val count: Int, override val next: String?, val results: List<Radio>) : FunkwhaleResponse<Radio>() {
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
  val cover: Covers
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
  val id: Int,
  val title: String,
  val artist: Artist,
  val album: Album,
  val position: Int,
  val uploads: List<Upload>
) : SearchResult {
  var current: Boolean = false
  var favorite: Boolean = false

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

  override fun cover() = album.cover.original
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
  val name: String,
  val description: String
)