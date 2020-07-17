package com.github.apognu.otter.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import com.github.apognu.otter.models.api.FunkwhaleRadio
import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.dao.RadioEntity
import com.github.apognu.otter.models.dao.toDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RadiosRepository(override val context: Context, private val database: OtterDatabase) : Repository<FunkwhaleRadio>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/radios/radios/?playable=true&ordering=name", FunkwhaleRadio.serializer())

  override fun onDataFetched(data: List<FunkwhaleRadio>): List<FunkwhaleRadio> {
    data.forEach {
      database.radios().insert(it.apply { radio_type = "custom" }.toDao())
    }

    return data
  }

  fun all(): LiveData<List<RadioEntity>> {
    scope.launch(Dispatchers.IO) {
      fetch().collect()
    }

    return database.radios().all()
  }
}