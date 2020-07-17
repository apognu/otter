package com.github.apognu.otter.repositories

import com.github.apognu.otter.models.dao.OtterDatabase
import com.github.apognu.otter.models.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class QueueRepository(private val database: OtterDatabase, private val scope: CoroutineScope) {
  fun all() = database.queue().allDecorated()

  fun allBlocking() = database.queue().allDecoratedBlocking()

  fun replace(tracks: List<Track>) = scope.launch {
    database.queue().replace(tracks)
  }
}