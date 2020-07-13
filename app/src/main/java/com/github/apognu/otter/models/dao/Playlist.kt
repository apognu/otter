package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.apognu.otter.models.api.FunkwhalePlaylist

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey
  val id: Int,
  val name: String,
  val album_covers: List<String>,
  val tracks_count: Int,
  val duration: Int
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM playlists ORDER BY name")
    fun all(): LiveData<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE id IN ( SELECT track_id FROM playlist_tracks WHERE playlist_id = :id )")
    fun tracksFor(id: Int): LiveData<List<DecoratedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addTracks(tracks: List<PlaylistTrack>)

    @Query("DELETE FROM playlist_tracks WHERE playlist_id = :id")
    fun deleteTracksFor(id: Int)

    @Transaction
    fun replaceTracks(id: Int, tracks: List<PlaylistTrack>) {
      deleteTracksFor(id)
      addTracks(tracks)
    }
  }
}

fun FunkwhalePlaylist.toDao(): PlaylistEntity = run {
  PlaylistEntity(id, name, album_covers, tracks_count, duration ?: 0)
}

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlist_id", "track_id"])
data class PlaylistTrack(
  val playlist_id: Int,
  val track_id: Int
)

object StringListConverter {
  @TypeConverter
  @JvmStatic
  fun fromString(value: String): List<String> {
    return value.split(",").toList()
  }

  @TypeConverter
  @JvmStatic
  fun toString(value: List<String>): String {
    return value.joinToString(",")
  }
}