package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.apognu.otter.models.api.FunkwhaleTrack

@Entity(tableName = "uploads")
data class UploadEntity(
  @PrimaryKey
  val listen_url: String,
  @ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE)
  val track_id: Int,
  val duration: Int,
  val bitrate: Int
) {

  @androidx.room.Dao
  interface Dao {
    @Query("SELECT * FROM uploads WHERE track_id IN ( :ids )")
    fun findAll(ids: List<Int>): LiveData<List<UploadEntity>>

    @Query("SELECT * FROM uploads WHERE track_id IN ( :ids )")
    suspend fun findAllBlocking(ids: List<Int>): List<UploadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(upload: UploadEntity)
  }
}

fun FunkwhaleTrack.FunkwhaleUpload.toDao(trackId: Int): UploadEntity = run {
  UploadEntity(
    listen_url,
    trackId,
    duration,
    bitrate
  )
}