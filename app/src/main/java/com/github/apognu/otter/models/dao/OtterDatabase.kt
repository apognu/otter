package com.github.apognu.otter.models.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  version = 1,
  entities = [
    ArtistEntity::class,
    AlbumEntity::class,
    TrackEntity::class,
    UploadEntity::class,
    QueueItemEntity::class,
    PlaylistEntity::class,
    PlaylistTrack::class,
    RadioEntity::class,
    FavoriteEntity::class
  ],
  views = [
    DecoratedArtistEntity::class,
    DecoratedAlbumEntity::class,
    DecoratedTrackEntity::class
  ]
)
@TypeConverters(StringListConverter::class)
abstract class OtterDatabase : RoomDatabase() {
  abstract fun artists(): ArtistEntity.Dao
  abstract fun albums(): AlbumEntity.Dao
  abstract fun tracks(): TrackEntity.Dao
  abstract fun uploads(): UploadEntity.Dao

  abstract fun queue(): QueueItemEntity.Dao

  abstract fun playlists(): PlaylistEntity.Dao
  abstract fun radios(): RadioEntity.Dao
  abstract fun favorites(): FavoriteEntity.Dao
}
