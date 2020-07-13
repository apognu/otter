package com.github.apognu.otter.repositories

import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class QueueRepository(val scope: CoroutineScope) {
  fun all() = Otter.get().database.queue().allDecorated()

  fun allBlocking() = Otter.get().database.queue().allDecoratedBlocking()

  fun replace(tracks: List<Track>) = scope.launch {
    Otter.get().database.queue().replace(tracks)
  }
}