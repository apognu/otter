package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.dao.RadioEntity

class RadiosViewModel : ViewModel() {
  companion object {
    private lateinit var instance: RadiosViewModel

    fun get(): RadiosViewModel {
      instance = if (::instance.isInitialized) instance else RadiosViewModel()

      return instance
    }
  }

  val radios: LiveData<List<RadioEntity>> by lazy { Otter.get().database.radios().all() }
}