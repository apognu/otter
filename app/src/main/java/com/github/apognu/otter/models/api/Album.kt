package com.github.apognu.otter.models.api

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.github.apognu.otter.utils.toCouchbaseArray
import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleAlbum(
  val id: Int,
  val artist: Artist,
  val title: String,
  val cover: Covers?,
  val release_date: String?
) {

  @Serializable
  data class Artist(val id: Int, val name: String)

  fun cover() = cover?.urls?.original

  companion object {
    fun persist(database: Database, albums: List<FunkwhaleAlbum>, base: Int? = null) {
      albums.forEachIndexed { index, album ->
        val doc = database.getDocument("album:${album.id}")?.toMutable() ?: MutableDocument("album:${album.id}")

        doc.run {
          setString("type", "album")

          setInt("id", album.id)
          setInt("artist_id", album.artist.id)
          setString("artist_name", album.artist.name)
          setString("title", album.title)
          setString("release_date", album.release_date)

          album.cover?.urls?.original?.let { cover ->
            setString("cover", cover)
          }

          base?.let {
            setInt("order", base + index)
          }

          database.save(this)
        }
      }
    }
  }
}

@Serializable
data class Covers(val urls: CoverUrls?)

@Serializable
data class CoverUrls(val original: String?)
