package com.github.apognu.otter.utils

import com.github.apognu.otter.Otter
import com.github.apognu.otter.models.dao.RadioEntity
import com.github.apognu.otter.models.domain.Track
import com.google.android.exoplayer2.offline.Download
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

sealed class Command {
  class StartService(val command: Command) : Command()
  object RefreshService : Command()

  object ToggleState : Command()
  class SetState(val state: Boolean) : Command()

  object NextTrack : Command()
  object PreviousTrack : Command()
  class Seek(val progress: Int) : Command()

  class AddToQueue(val tracks: List<Track>) : Command()
  class PlayNext(val track: Track) : Command()
  class ReplaceQueue(val queue: List<Track>, val fromRadio: Boolean = false) : Command()
  class RemoveFromQueue(val track: Track) : Command()
  class MoveFromQueue(val oldPosition: Int, val newPosition: Int) : Command()
  object ClearQueue : Command()
  object ShuffleQueue : Command()
  class PlayRadio(val radio: RadioEntity) : Command()

  class SetRepeatMode(val mode: Int) : Command()

  class PlayTrack(val index: Int) : Command()
  class PinTrack(val track: Track) : Command()
  class PinTracks(val tracks: List<Track>) : Command()

  class RefreshTrack(val track: Track?) : Command()
}

sealed class Event {
  object LogOut : Event()

  class PlaybackError(val message: String) : Event()
  object PlaybackStopped : Event()
  class TrackFinished(val track: Track?) : Event()
  object RadioStarted : Event()
  object ListingsChanged : Event()
  class DownloadChanged(val download: Download) : Event()
}

object EventBus {
  fun send(event: Event) {
    GlobalScope.launch(IO) {
      Otter.get().eventBus.offer(event)
    }
  }

  fun get() = Otter.get().eventBus.asFlow()
}

object CommandBus {
  fun send(command: Command) {
    GlobalScope.launch(IO) {
      Otter.get().commandBus.offer(command)
    }
  }

  fun get() = Otter.get().commandBus.asFlow()
}

