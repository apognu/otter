package com.github.apognu.otter.utils

import com.github.apognu.otter.Otter
import com.google.android.exoplayer2.offline.DownloadCursor
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

sealed class Command {
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
  class PlayRadio(val radio: Radio) : Command()

  class SetRepeatMode(val mode: Int) : Command()

  class PlayTrack(val index: Int) : Command()
  class PinTrack(val track: Track) : Command()
  class PinTracks(val tracks: List<Track>) : Command()
}

sealed class Event {
  object LogOut : Event()

  class PlaybackError(val message: String) : Event()
  object PlaybackStopped : Event()
  class Buffering(val value: Boolean) : Event()
  class TrackPlayed(val track: Track?, val play: Boolean) : Event()
  class TrackFinished(val track: Track?) : Event()
  class RefreshTrack(val track: Track?, val play: Boolean) : Event()
  class StateChanged(val playing: Boolean) : Event()
  object QueueChanged : Event()
  object RadioStarted : Event()
  object ListingsChanged : Event()
  object DownloadChanged : Event()
}

sealed class Request(var channel: Channel<Response>? = null) {
  object GetState : Request()
  object GetQueue : Request()
  object GetCurrentTrack : Request()
  object GetDownloads : Request()
}

sealed class Response {
  class State(val playing: Boolean) : Response()
  class Queue(val queue: List<Track>) : Response()
  class CurrentTrack(val track: Track?) : Response()
  class Downloads(val cursor: DownloadCursor) : Response()
}

object EventBus {
  fun send(event: Event) {
    GlobalScope.launch {
      Otter.get().eventBus.send(event)
    }
  }

  fun get() = Otter.get().eventBus.asFlow()
}

object CommandBus {
  fun send(command: Command) {
    GlobalScope.launch {
      get().offer(command)
    }
  }

  fun get() = Otter.get().commandBus
}

object RequestBus {
  fun send(request: Request): Channel<Response> {
    return Channel<Response>().also {
      GlobalScope.launch(Main) {
        request.channel = it

        Otter.get().requestBus.offer(request)
      }
    }
  }

  fun get() = Otter.get().requestBus.asFlow()
}

object ProgressBus {
  fun send(current: Int, duration: Int, percent: Int) {
    GlobalScope.launch {
      Otter.get().progressBus.send(Triple(current, duration, percent))
    }
  }

  fun get() = Otter.get().progressBus.asFlow().conflate()
}

suspend inline fun <reified T> Channel<Response>.wait(): T? {
  return when (val response = this.receive()) {
    is T -> response
    else -> null
  }
}
