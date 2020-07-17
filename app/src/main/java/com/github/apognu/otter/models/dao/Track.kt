package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import org.koin.java.KoinJavaComponent.inject

@Entity(tableName = "tracks")
data class TrackEntity(
  @PrimaryKey
  val id: Int,
  val title: String,
  @ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"], onDelete = CASCADE)
  val artist_id: Int,
  @ForeignKey(entity = AlbumEntity::class, parentColumns = ["id"], childColumns = ["album_id"], onDelete = CASCADE)
  val album_id: Int?,
  val position: Int?,
  val copyright: String?,
  val license: String?
) {

  @androidx.room.Dao
  interface Dao {
    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE id = :id")
    fun find(id: Int): DecoratedTrackEntity

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE id IN ( :ids )")
    fun findAllDecorated(ids: List<Int>): LiveData<List<DecoratedTrackEntity>>

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE id = :id")
    fun getDecorated(id: Int): LiveData<DecoratedTrackEntity>

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE id = :id")
    fun getDecoratedBlocking(id: Int): DecoratedTrackEntity

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE album_id IN ( :albumIds )")
    fun ofAlbumsDecorated(albumIds: List<Int>): LiveData<List<DecoratedTrackEntity>>

    @Transaction
    @Query("SELECT * FROM DecoratedTrackEntity WHERE artist_id = :artistId")
    suspend fun ofArtistBlocking(artistId: Int): List<DecoratedTrackEntity>

    @Transaction
    @Query("""
      SELECT tracks.*
      FROM DecoratedTrackEntity tracks
      INNER JOIN favorites
      WHERE favorites.track_id = tracks.id
    """)
    fun favorites(): LiveData<List<DecoratedTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(track: TrackEntity)

    @Transaction
    fun insertWithAssocs(artistsDao: ArtistEntity.Dao, albumsDao: AlbumEntity.Dao, uploadsDao: UploadEntity.Dao, track: FunkwhaleTrack) {
      artistsDao.insert(track.artist.toDao())

      track.album?.let {
        albumsDao.insert(it.toDao())
      }

      insert(track.toDao())

      track.uploads.forEach {
        uploadsDao.insert(it.toDao(track.id))
      }
    }
  }
}

fun FunkwhaleTrack.toDao() = run {
  TrackEntity(
    id,
    title,
    artist.id,
    album?.id,
    position,
    copyright,
    license
  )
}

@DatabaseView("""
  SELECT
    tracks.id, tracks.title, tracks.position, tracks.copyright, tracks.license,
    ar.id AS artist_id, ar.name AS artist_name, ar.album_count AS artist_album_count, ar.album_cover AS artist_album_cover,
    al.id AS album_id, al.title AS album_title, al.artist_id AS album_artist_id, al.cover AS album_cover, al.release_date AS album_release_date, al.artist_name AS album_artist_name,
    CASE
      WHEN favorites.track_id IS NULL THEN 0
      ELSE 1
    END AS favorite
  FROM tracks
  LEFT JOIN DecoratedAlbumEntity al
  ON al.id = tracks.album_id
  LEFT JOIN DecoratedArtistEntity ar
  ON ar.id = al.artist_id
  LEFT JOIN favorites
  ON favorites.track_id = tracks.id
  ORDER BY tracks.position
""")
data class DecoratedTrackEntity(
  val id: Int,
  val title: String,
  val position: Int?,
  val copyright: String?,
  val license: String?,

  // Virtual attributes
  val favorite: Boolean,

  // Associations
  @Embedded(prefix = "artist_")
  val artist: DecoratedArtistEntity?,
  @Embedded(prefix = "album_")
  val album: DecoratedAlbumEntity?,
  @Relation(entityColumn = "track_id", parentColumn = "id")
  val uploads: List<UploadEntity>
)