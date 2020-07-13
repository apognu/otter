package com.github.apognu.otter.models.api

import kotlinx.serialization.Serializable

@Serializable
data class FunkwhalePlaylist(
  val id: Int,
  val name: String,
  val album_covers: List<String>,
  val tracks_count: Int,
  val duration: Int?
)

@Serializable
data class FunkwhalePlaylistTrack(val track: FunkwhaleTrack)