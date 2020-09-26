package com.github.apognu.otter.models.api

import com.couchbase.lite.Database
import com.couchbase.lite.MutableDocument
import com.github.apognu.otter.models.domain.SearchResult
import com.github.apognu.otter.utils.toCouchbaseArray
import com.google.android.exoplayer2.offline.Download
import kotlinx.serialization.ContextualSerialization
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

    fun persist(database: Database, tracks: List<FunkwhaleTrack>, base: Int? = null) {
      tracks.forEachIndexed { index, track ->
        val artistDoc = database.getDocument("artist:${track.artist.id}")?.toMutable() ?: MutableDocument("artist:${track.artist.id}")

        artistDoc.run {
          setString("type", "artist")

          setInt("id", track.artist.id)
          setString("name", track.artist.name)

          database.save(this)
        }

        track.album?.let { album ->
          val albumDoc = database.getDocument("album:${album.id}")?.toMutable() ?: MutableDocument("album:${album.id}")

          albumDoc.run {
            setString("type", "album")

            setInt("id", album.id)
            setInt("artist_id", album.artist.id)
            setString("artist_name", album.artist.name)
            setString("title", album.title)
            setString("release_date", album.release_date)

            album.cover?.urls?.original?.let { cover ->
              setString("cover", cover)
            }

            database.save(this)
          }
        }

        val doc = database.getDocument("track:${track.id}")?.toMutable() ?: MutableDocument("track:${track.id}")

        doc.run {
          setString("type", "track")

          setInt("id", track.id)

          track.album?.let { album ->
            setInt("albumId", album.id)
          }

          setInt("artistId", track.artist.id)
          setString("title", track.title)
          setInt("position", track.position)
          setInt("disc_number", track.disc_number ?: 0)
          setString("copyright", track.copyright)
          setString("license", track.license)
          setString("uploads", track.uploads.getOrNull(0)?.listen_url ?: "")

          database.save(this)
        }
      }
    }
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

@Serializable
data class DownloadInfo(
  val id: Int,
  val contentId: String,
  val title: String,
  val artist: String,
  @ContextualSerialization
  var download: Download?
)