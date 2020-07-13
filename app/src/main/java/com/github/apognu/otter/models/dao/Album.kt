package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.api.FunkwhaleArtist

@Entity(tableName = "albums")
data class AlbumEntity(
  @PrimaryKey
  val id: Int,
  val title: String,
  @ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artist_id"], onDelete = ForeignKey.CASCADE)
  val artist_id: Int,
  val cover: String?,
  val release_date: String?
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM DecoratedAlbumEntity")
    fun allDecorated(): LiveData<List<DecoratedAlbumEntity>>

    @Query("SELECT * FROM DecoratedAlbumEntity ORDER BY release_date")
    fun allSync(): List<DecoratedAlbumEntity>

    @Query("SELECT * FROM DecoratedAlbumEntity WHERE id IN ( :ids ) ORDER BY release_date")
    fun findAllDecorated(ids: List<Int>): LiveData<List<DecoratedAlbumEntity>>

    @Query("SELECT * FROM DecoratedAlbumEntity WHERE id == :id")
    fun getDecorated(id: Int): LiveData<DecoratedAlbumEntity>

    @Query("SELECT * FROM DecoratedAlbumEntity WHERE id == :id")
    fun getDecoratedBlocking(id: Int): DecoratedAlbumEntity

    @Query("SELECT * FROM DecoratedAlbumEntity WHERE artist_id = :artistId")
    fun forArtistDecorated(artistId: Int): LiveData<List<DecoratedAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(album: AlbumEntity)
  }
}

fun FunkwhaleAlbum.toDao() = run {
  AlbumEntity(id, title, artist.id, cover(), release_date)
}

fun FunkwhaleArtist.Album.toDao(artistId: Int) = run {
  AlbumEntity(id, title, artistId, cover?.urls?.original, release_date)
}

@DatabaseView("""
  SELECT albums.*, artists.name AS artist_name
  FROM albums
  INNER JOIN artists
  ON artists.id = albums.artist_id
  ORDER BY albums.release_date
""")
data class DecoratedAlbumEntity(
  val id: Int,
  val title: String,
  val artist_id: Int,
  val cover: String?,
  val release_date: String?,
  val artist_name: String
)
