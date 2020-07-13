package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleTrack
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.runBlocking

class ArtistTracksRepository(override val context: Context?, private val artistId: Int) : Repository<FunkwhaleTrack>() {
  override val upstream = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&artist=$artistId", FunkwhaleTrack.serializer())

  override fun onDataFetched(data: List<FunkwhaleTrack>) = runBlocking(IO) {
    data.forEach {
      Otter.get().database.tracks().insertWithAssocs(it)
    }

    super.onDataFetched(data)
  }
}