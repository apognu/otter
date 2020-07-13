package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.dao.toDao
import com.github.apognu.otter.models.dao.toRealmDao
import io.realm.Realm
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

class ArtistsRepository(override val context: Context?) : Repository<FunkwhaleArtist>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/artists/?playable=true&ordering=name", FunkwhaleArtist.serializer())

  override fun onDataFetched(data: List<FunkwhaleArtist>): List<FunkwhaleArtist> {
    scope.launch(IO) {
      data.forEach { artist ->
        Otter.get().database.artists().insert(artist.toDao())

        Realm.getDefaultInstance().executeTransaction { realm ->
          realm.insertOrUpdate(artist.toRealmDao())
        }

        artist.albums?.forEach { album ->
          Otter.get().database.albums().insert(album.toDao(artist.id))
        }
      }
    }

    return super.onDataFetched(data)
  }
}