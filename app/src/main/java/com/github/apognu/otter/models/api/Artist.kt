package com.github.apognu.otter.models.api

import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleArtist(
  val id: Int,
  val name: String,
  val albums: List<Album>? = null
) {

  @Serializable
  data class Album(
    val id: Int,
    val title: String,
    val cover: Covers?,
    val release_date: String?
  )
}
