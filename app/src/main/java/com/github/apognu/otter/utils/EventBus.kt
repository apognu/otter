package com.github.apognu.otter.utils

import com.github.apognu.otter.Otter
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.filter
import kotlinx.coroutines.channels.map
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
  class ReplaceQueue(val queue: List<Track>) : Command()
  class RemoveFromQueue(val track: Track) : Command()
  class MoveFromQueue(val oldPosition: Int, val newPosition: Int) : Command()

  class PlayTrack(val index: Int) : Command()
}

sealed class Event {
  object LogOut : Event()

  class PlaybackError(val message: String) : Event()
  object PlaybackStopped : Event()
  class Buffering(val value: Boolean) : Event()
  class TrackPlayed(val track: Track?, val play: Boolean) : Event()
  class StateChanged(val playing: Boolean) : Event()
  object QueueChanged : Event()
}

sealed class Request(var channel: Channel<Response>? = null) {
  object GetState : Request()
  object GetQueue : Request()
  object GetCurrentTrack : Request()
}

sealed class Response {
  class State(val playing: Boolean) : Response()
  class Queue(val queue: List<Track>) : Response()
  class CurrentTrack(val track: Track?) : Response()
}

object EventBus {
  fun send(event: Event) {
    GlobalScope.launch {
      get().offer(event)
    }
  }

  fun get() = Otter.get().eventBus

  inline fun <reified T : Event> asChannel(): ReceiveChannel<T> {
    return get().openSubscription().filter { it is T }.map { it as T }
  }
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

        get().offer(request)
      }
    }
  }

  fun get() = Otter.get().requestBus

  inline fun <reified T> asChannel(): ReceiveChannel<T> {
    return get().openSubscription().filter { it is T }.map { it as T }
  }
}

object ProgressBus {
  fun send(current: Int, duration: Int, percent: Int) {
    GlobalScope.launch {
      Otter.get().progressBus.send(Triple(current, duration, percent))
    }
  }

  fun asChannel(): ReceiveChannel<Triple<Int, Int, Int>> {
    return Otter.get().progressBus.openSubscription()
  }
}

suspend inline fun <reified T> Channel<Response>.wait(): T? {
  return when (val response = this.receive()) {
    is T -> response
    else -> null
  }
}
