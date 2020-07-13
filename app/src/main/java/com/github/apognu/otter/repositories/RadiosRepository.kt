package com.github.apognu.otter.repositories

import android.content.Context
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.api.FunkwhaleRadio
import com.github.apognu.otter.models.dao.toDao

class RadiosRepository(override val context: Context?) : Repository<FunkwhaleRadio>() {
  override val upstream =
    HttpUpstream(HttpUpstream.Behavior.Progressive, "/api/v1/radios/radios/?playable=true&ordering=name", FunkwhaleRadio.serializer())

  override fun onDataFetched(data: List<FunkwhaleRadio>): List<FunkwhaleRadio> {
    data.forEach {
      Otter.get().database.radios().insert(it.toDao())
    }

    return data
      .map { radio -> radio.apply { radio_type = "custom" } }
      .toMutableList()
  }
}