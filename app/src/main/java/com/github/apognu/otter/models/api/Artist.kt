package com.github.apognu.otter.models.api

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.github.apognu.otter.utils.toCouchbaseArray
import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleArtist(
  val id: Int,
  val name: String,
  val albums: List<Album>? = null
) {

  @Serializable
  data class Album(
    val id: Int,
    val title: String,
    val cover: Covers?,
    val release_date: String?
  )

  companion object {
    fun persist(database: Database, artists: List<FunkwhaleArtist>, base: Int? = null) {
      artists
        // .filter { it.albums?.isNotEmpty() ?: false }
        .forEachIndexed { index, artist ->
          artist.albums?.forEach { album ->
            val albumDoc = database.getDocument("album:${album.id}")?.toMutable() ?: MutableDocument("album:${album.id}")

            albumDoc.run {
              setString("type", "album")

              setInt("id", album.id)
              setInt("artist_id", artist.id)
              setString("artist_name", artist.name)
              setString("title", album.title)
              setString("release_date", album.release_date)

              album.cover?.urls?.original?.let { cover ->
                setString("cover", cover)
              }

              database.save(this)
            }
          }

          val artistDoc = database.getDocument("artist:${artist.id}")?.toMutable() ?: MutableDocument("artist:${artist.id}")

          artistDoc.run {
            setString("type", "artist")

            setInt("id", artist.id)
            setString("name", artist.name)
            setArray("albums", artist.albums?.map { it.id }.toCouchbaseArray())

            artist.albums?.getOrNull(0)?.cover?.urls?.original?.let { cover ->
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
