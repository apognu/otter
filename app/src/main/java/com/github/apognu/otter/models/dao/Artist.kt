package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.github.apognu.otter.models.api.FunkwhaleArtist
import io.realm.RealmObject
import io.realm.annotations.Required

@Entity(tableName = "artists")
data class ArtistEntity(
  @PrimaryKey
  val id: Int,
  @ColumnInfo(collate = ColumnInfo.UNICODE, index = true)
  val name: String
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM DecoratedArtistEntity")
    fun allPaged(): DataSource.Factory<Int, DecoratedArtistEntity>

    @Query("SELECT * FROM DecoratedArtistEntity")
    fun allDecorated(): LiveData<List<DecoratedArtistEntity>>

    @Query("SELECT * FROM DecoratedArtistEntity WHERE id == :id")
    fun getDecorated(id: Int): LiveData<DecoratedArtistEntity>

    @Query("SELECT * FROM DecoratedArtistEntity WHERE id == :id")
    fun getDecoratedBlocking(id: Int): DecoratedArtistEntity

    @Query("SELECT * FROM DecoratedArtistEntity WHERE id IN ( :ids )")
    fun findDecorated(ids: List<Int>): LiveData<List<DecoratedArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(artist: ArtistEntity)

    @Query("DELETE FROM artists")
    fun deleteAll()
  }
}

fun FunkwhaleArtist.toDao() = run {

  ArtistEntity(id, name)
}

@DatabaseView("""
  SELECT artists.id, artists.name, COUNT(*) AS album_count, albums.cover AS album_cover
  FROM artists
  INNER JOIN albums
  ON albums.artist_id = artists.id
  GROUP BY albums.artist_id
  ORDER BY artists.id
""")
data class DecoratedArtistEntity(
  val id: Int,
  val name: String,
  val album_count: Int,
  val album_cover: String?
)

open class RealmArtist(
  @io.realm.annotations.PrimaryKey
  var id: Int = 0,
  @Required
  var name: String = ""
) : RealmObject()

fun FunkwhaleArtist.toRealmDao() = run {
  RealmArtist(id, name)
}