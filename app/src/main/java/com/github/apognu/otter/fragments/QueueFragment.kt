package com.github.apognu.otter.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.TracksAdapter
import com.github.apognu.otter.utils.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.android.synthetic.main.fragment_queue.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class QueueFragment : BottomSheetDialogFragment() {
  private var adapter: TracksAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FloatingBottomSheet)

    watchEventBus()
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState).apply {
      setOnShowListener {
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
          BottomSheetBehavior.from(it).skipCollapsed = true
        }
      }
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_queue, container, false).apply {
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
    GlobalScope.launch(Main) {
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
    GlobalScope.launch(Main) {
      for (message in EventBus.asChannel<Event>()) {
        when (message) {
          is Event.TrackPlayed -> refresh()
          is Event.QueueChanged -> refresh()
        }
      }
    }
  }
}