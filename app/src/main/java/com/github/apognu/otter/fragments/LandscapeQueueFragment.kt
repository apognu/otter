package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.utils.*
import kotlinx.android.synthetic.main.partial_queue.*
import kotlinx.android.synthetic.main.partial_queue.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LandscapeQueueFragment : Fragment() {
  private var adapter: TracksAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    watchEventBus()
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.partial_queue, container, false).apply {
      adapter = TracksAdapter(context, fromQueue = true).also {
        queue.layoutManager = LinearLayoutManager(context)
        queue.adapter = it
      }
    }
  }

  override fun onResume() {
    super.onResume()

    queue?.visibility = View.GONE
    placeholder?.visibility = View.VISIBLE

    refresh()
  }

  private fun refresh() {
    activity?.lifecycleScope?.launch(Main) {
      RequestBus.send(Request.GetQueue).wait<Response.Queue>()?.let { response ->
        adapter?.let {
          it.data = response.queue.toMutableList()
          it.notifyDataSetChanged()

          if (it.data.isEmpty()) {
            queue?.visibility = View.GONE
            placeholder?.visibility = View.VISIBLE
          } else {
            queue?.visibility = View.VISIBLE
            placeholder?.visibility = View.GONE
          }
        }
      }
    }
  }

  private fun watchEventBus() {
    activity?.lifecycleScope?.launch(Main) {
      EventBus.get().collect { message ->
        when (message) {
          is Event.QueueChanged -> refresh()
        }
      }
    }

    activity?.lifecycleScope?.launch(Main) {
      CommandBus.get().collect { command ->
        when (command) {
          is Command.RefreshTrack -> refresh()
        }
      }
    }
  }
}