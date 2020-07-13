package com.github.apognu.otter.models.domain

import com.github.apognu.otter.models.dao.UploadEntity

data class Upload(
  val listen_url: String,
  val track_id: Int,
  val duration: Int,
  val bitrate: Int
) {

  companion object {
    fun fromEntity(upload: UploadEntity): Upload = upload.run {
      Upload(
        listen_url,
        track_id,
        duration,
        bitrate
      )
    }
  }
}