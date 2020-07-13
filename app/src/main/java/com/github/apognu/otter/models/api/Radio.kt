package com.github.apognu.otter.models.api

import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleRadio(
  val id: Int,
  var radio_type: String? = null,
  val name: String,
  val description: String,
  var related_object_id: String? = null
)