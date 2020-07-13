package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class OtterAdapter<D, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
  var data: MutableList<D> = mutableListOf()

  init {
    super.setHasStableIds(true)
  }

  abstract override fun getItemId(position: Int): Long
}

abstract class LiveOtterFragment<D : Any, DAO : Any, A : OtterAdapter<DAO, *>> : Fragment() {
  companion object {
    const val OFFSCREEN_PAGES = 20
  }

  abstract val liveData: LiveData<List<DAO>>
  abstract val viewRes: Int
  abstract val recycler: RecyclerView
  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)
  open val alwaysRefresh = true

  lateinit var repository: Repository<D>
  lateinit var adapter: A

  private var moreLoading = false
  private var listener: Job? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler.layoutManager = layoutManager
    (recycler.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    recycler.adapter = adapter

    (repository.upstream as? HttpUpstream<*>)?.let { upstream ->
      if (upstream.behavior == HttpUpstream.Behavior.Progressive) {
        recycler.setOnScrollChangeListener { _, _, _, _, _ ->
          val offset = recycler.computeVerticalScrollOffset()

          if (!moreLoading && offset > 0 && needsMoreOffscreenPages()) {
            moreLoading = true

            fetch(adapter.data.size)
          }
        }
      }
    }

    if (listener == null) {
      listener = lifecycleScope.launch(IO) {
        EventBus.get().collect { event ->
          if (event is Event.ListingsChanged) {
            withContext(Main) {
              swiper?.isRefreshing = true
              fetch()
            }
          }
        }
      }
    }

    fetch()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    liveData.observe(this) {
      adapter.data = it.toMutableList()
      adapter.notifyDataSetChanged()
    }
  }

  override fun onResume() {
    super.onResume()

    swiper?.setOnRefreshListener {
      fetch()
    }
  }

  open fun onDataFetched(data: List<D>) {}

  private fun fetch(size: Int = 0) {
    moreLoading = true

    repository.fetch(size).untilNetwork(lifecycleScope, IO) { data, _, hasMore ->
      lifecycleScope.launch(Main) {
        onDataFetched(data)

        if (hasMore) {
          (repository.upstream as? HttpUpstream<*>)?.let { upstream ->
            if (upstream.behavior == HttpUpstream.Behavior.Progressive) {
              if (size == 0 || needsMoreOffscreenPages()) {
                fetch(size + data.size)
              } else {
                moreLoading = false
              }
            }
          }
        }

        (repository.upstream as? HttpUpstream<*>)?.let { upstream ->
          when (upstream.behavior) {
            HttpUpstream.Behavior.Progressive -> if (!hasMore || !moreLoading) swiper?.isRefreshing = false
            HttpUpstream.Behavior.AtOnce -> if (!hasMore) swiper?.isRefreshing = false
            HttpUpstream.Behavior.Single -> if (!hasMore) swiper?.isRefreshing = false
          }
        }
      }
    }
  }

  private fun needsMoreOffscreenPages(): Boolean {
    view?.let {
      val offset = recycler.computeVerticalScrollOffset()
      val left = recycler.computeVerticalScrollRange() - recycler.height - offset

      return left < (recycler.height * OFFSCREEN_PAGES)
    }

    return false
  }
}