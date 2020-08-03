package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.FunkwhaleArtist
import com.github.apognu.otter.models.dao.DecoratedArtistEntity
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.toDao
import com.github.apognu.otter.models.dao.toRealmDao
import io.realm.Realm
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ArtistsRepository(override val context: Context, private val database: OtterDatabase) : Repository<FunkwhaleArtist>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/artists/?playable=true&ordering=id", FunkwhaleArtist.serializer())

  override fun onDataFetched(data: List<FunkwhaleArtist>): List<FunkwhaleArtist> {
    scope.launch(IO) {
      data.forEach { artist ->
        database.artists().insert(artist.toDao())

        Realm.getDefaultInstance().executeTransaction { realm ->
          realm.insertOrUpdate(artist.toRealmDao())
        }

        artist.albums?.forEach { album ->
          database.albums().insert(album.toDao(artist.id))
        }
      }
    }

    return super.onDataFetched(data)
  }

  fun insert(artist: FunkwhaleArtist) = database.artists().insert(artist.toDao())

  fun allPaged() = database.artists().allPaged()

  fun all(): LiveData<List<DecoratedArtistEntity>> {
    scope.launch(IO) {
      fetch().collect()
    }

    return database.artists().allDecorated()
  }

  fun get(id: Int) = database.artists().getDecorated(id)
  fun find(ids: List<Int>) = database.artists().findDecorated(ids)
}