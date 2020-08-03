package com.github.apognu.otter.models.api

import kotlinx.serialization.Serializable

@Serializable
data class FunkwhaleAlbum(
  val id: Int,
  val artist: Artist,
  val title: String,
  val cover: Covers?,
  val release_date: String?
) {

  @Serializable
  data class Artist(val id: Int, val name: String)

  fun cover() = cover?.urls?.original
}

@Serializable
data class Covers(val urls: CoverUrls?)

@Serializable
data class CoverUrls(val original: String?)

