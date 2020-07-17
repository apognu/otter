package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.models.api.FunkwhaleTrack
import com.github.apognu.otter.models.dao.OtterDatabase
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking

class ArtistTracksRepository(override val context: Context, private val database: OtterDatabase, artistId: Int) : Repository<FunkwhaleTrack>() {
  override val upstream = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&artist=$artistId", FunkwhaleTrack.serializer())

  override fun onDataFetched(data: List<FunkwhaleTrack>) = runBlocking(IO) {
    data.forEach {
      database.tracks().insertWithAssocs(database.artists(), database.albums(), database.uploads(), it)
    }

    super.onDataFetched(data)
  }
}