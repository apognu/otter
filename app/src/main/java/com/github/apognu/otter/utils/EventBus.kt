package com.github.apognu.otter.utils

import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.*
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
  private var bus: BroadcastChannel<Event> = BroadcastChannel(10)

  fun send(event: Event) {
    GlobalScope.launch {
      bus.offer(event)
    }
  }

  fun get() = bus

  inline fun <reified T : Event> asChannel(): ReceiveChannel<T> {
    return get().openSubscription().filter { it is T }.map { it as T }
  }
}

object CommandBus {
  private var bus: Channel<Command> = Channel(10)

  fun send(command: Command) {
    GlobalScope.launch {
      bus.offer(command)
    }
  }

  fun asChannel() = bus
}

object RequestBus {
  private var bus: BroadcastChannel<Request> = BroadcastChannel(10)

  fun send(request: Request): Channel<Response> {
    return Channel<Response>().also {
      GlobalScope.launch(Main) {
        request.channel = it

        bus.offer(request)
      }
    }
  }

  fun get() = bus

  inline fun <reified T> asChannel(): ReceiveChannel<T> {
    return get().openSubscription().filter { it is T }.map { it as T }
  }
}

object ProgressBus {
  private val bus: BroadcastChannel<Triple<Int, Int, Int>> = ConflatedBroadcastChannel()

  fun send(current: Int, duration: Int, percent: Int) {
    GlobalScope.launch {
      bus.send(Triple(current, duration, percent))
    }
  }

  fun asChannel(): ReceiveChannel<Triple<Int, Int, Int>> {
    return bus.openSubscription()
  }
}

suspend inline fun <reified T> Channel<Response>.wait(): T? {
  return when (val response = this.receive()) {
    is T -> response
    else -> null
  }
}
