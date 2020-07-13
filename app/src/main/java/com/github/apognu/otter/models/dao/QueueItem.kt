package com.github.apognu.otter.models.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.github.apognu.otter.models.domain.Track

@Entity(tableName = "queue")
data class QueueItemEntity(
  @PrimaryKey
  val position: Int,
  @ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE)
  val trackId: Int
) {

  @androidx.room.Dao
  interface Dao {
    @Transaction
    @Query("""
      SELECT tracks.*
      FROM DecoratedTrackEntity tracks
      INNER JOIN queue
      ON queue.trackId = tracks.id
      ORDER BY queue.position
    """)
    fun allDecorated(): LiveData<List<DecoratedTrackEntity>>

    @Transaction
    @Query("""
      SELECT tracks.*
      FROM DecoratedTrackEntity tracks
      INNER JOIN queue
      ON queue.trackId = tracks.id
      ORDER BY queue.position
    """)
    fun allDecoratedBlocking(): List<DecoratedTrackEntity>

    @Query("DELETE FROM queue")
    fun empty()

    @Insert
    fun insertAll(tracks: List<QueueItemEntity>)

    @Transaction
    fun replace(tracks: List<Track>) {
      empty()
      insertAll(tracks.mapIndexed { position, track ->
        track.toQueueItemDao(position)
      })
    }
  }
}

fun Track.toQueueItemDao(position: Int = 0): QueueItemEntity = run {
  QueueItemEntity(position, id)
}