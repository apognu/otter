package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.models.api.FunkwhaleAlbum
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.api.FunkwhaleTrack
import kotlinx.coroutines.runBlocking

class TracksSearchRepository(override val context: Context?, var query: String) : Repository<FunkwhaleTrack>() {
  override val upstream: Upstream<FunkwhaleTrack>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/tracks/?playable=true&q=$query", FunkwhaleTrack.serializer())

  override fun onDataFetched(data: List<FunkwhaleTrack>): List<FunkwhaleTrack> = runBlocking {
    /* val downloaded = TracksRepository.getDownloadedIds() ?: listOf()

    data.map { track ->
      track.favorite = favorites.contains(track.id)
      track.downloaded = downloaded.contains(track.id)

      track.bestUpload()?.let { upload ->
        val url = mustNormalizeUrl(upload.listen_url)

        track.cached = Otter.get().exoCache.isCached(url, 0, upload.duration * 1000L)
      }

      track
    } */

    data
  }
}

class ArtistsSearchRepository(override val context: Context?, var query: String) : Repository<FunkwhaleArtist>() {
  override val upstream: Upstream<FunkwhaleArtist>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/artists/?playable=true&q=$query", FunkwhaleArtist.serializer())
}

class AlbumsSearchRepository(override val context: Context?, var query: String) : Repository<FunkwhaleAlbum>() {
  override val upstream: Upstream<FunkwhaleAlbum>
    get() = HttpUpstream(HttpUpstream.Behavior.AtOnce, "/api/v1/albums/?playable=true&q=$query", FunkwhaleAlbum.serializer())
}