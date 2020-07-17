package com.github.apognu.otter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.dao.RadioEntity
import com.github.apognu.otter.repositories.RadiosRepository

class RadiosViewModel(private val repository: RadiosRepository) : ViewModel() {
  val radios: LiveData<List<RadioEntity>> by lazy { repository.all() }
}