package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(
  @PrimaryKey
  val track_id: Int
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM favorites")
    fun all(): LiveData<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(trackId: FavoriteEntity)

    @Query("INSERT OR REPLACE INTO favorites VALUES ( :trackId )")
    fun add(trackId: Int)

    @Query("DELETE FROM favorites WHERE track_id = :trackId")
    fun remove(trackId: Int)
  }
}
