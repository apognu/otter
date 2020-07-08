package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.untilNetwork
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class FunkwhaleAdapter<D, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
  var data: MutableList<D> = mutableListOf()
}

abstract class FunkwhaleFragment<D : Any, A : FunkwhaleAdapter<D, *>> : Fragment() {
  val INITIAL_PAGES = 5
  val OFFSCREEN_PAGES = 10

  abstract val viewRes: Int
  abstract val recycler: RecyclerView
  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)
  open val alwaysRefresh = true

  lateinit var repository: Repository<D, *>
  lateinit var adapter: A

  private var initialFetched = false
  private var moreLoading = false
  private var listener: Job? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler.layoutManager = layoutManager
    recycler.adapter = adapter

    (repository.upstream as? HttpUpstream<*, *>)?.let { upstream ->
      if (upstream.behavior == HttpUpstream.Behavior.Progressive) {
        recycler.setOnScrollChangeListener { _, _, _, _, _ ->
          val offset = recycler.computeVerticalScrollOffset()
          val left = recycler.computeVerticalScrollRange() - recycler.height - offset

          if (initialFetched && !moreLoading && offset > 0 && left < (recycler.height * OFFSCREEN_PAGES)) {
            moreLoading = true
            fetch(Repository.Origin.Network.origin, adapter.data.size)
          }
        }
      }
    }

    fetch(Repository.Origin.Cache.origin)

    if (alwaysRefresh && adapter.data.isEmpty()) {
      fetch(Repository.Origin.Network.origin)
    }
  }

  override fun onResume() {
    super.onResume()

    swiper?.setOnRefreshListener {
      fetch(Repository.Origin.Network.origin)
    }

    if (listener == null) {
      listener = lifecycleScope.launch(IO) {
        EventBus.get().collect { event ->
          if (event is Event.ListingsChanged) {
            withContext(Main) {
              swiper?.isRefreshing = true
              fetch(Repository.Origin.Network.origin)
            }
          }
        }
      }
    }
  }

  open fun onDataFetched(data: List<D>) {}

  private fun fetch(upstreams: Int = Repository.Origin.Network.origin, size: Int = 0) {
    var first = size == 0

    if (!moreLoading && upstreams == Repository.Origin.Network.origin) {
      swiper?.isRefreshing = true
    }

    repository.fetch(upstreams, size).untilNetwork(lifecycleScope, IO) { data, isCache, page, hasMore ->
      if (isCache && data.isEmpty()) {
        return@untilNetwork fetch(Repository.Origin.Network.origin)
      }

      lifecycleScope.launch(Main) {
        if (isCache) {
          adapter.data = data.toMutableList()
          adapter.notifyDataSetChanged()

          return@launch
        }

        if (first && data.isNotEmpty()) {
          adapter.data.clear()
        }

        onDataFetched(data)

        adapter.data.addAll(data)

        (repository.upstream as? HttpUpstream<*, *>)?.let { upstream ->
          when (upstream.behavior) {
            HttpUpstream.Behavior.Progressive -> if (!hasMore || page >= INITIAL_PAGES) swiper?.isRefreshing = false
            HttpUpstream.Behavior.AtOnce -> if (!hasMore) swiper?.isRefreshing = false
            HttpUpstream.Behavior.Single -> if (!hasMore) swiper?.isRefreshing = false
          }
        }

        withContext(IO) {
          if (adapter.data.isNotEmpty()) {
            try {
              repository.cacheId?.let { cacheId ->
                Cache.set(
                  context,
                  cacheId,
                  Gson().toJson(repository.cache(adapter.data)).toByteArray()
                )
              }
            } catch (e: ConcurrentModificationException) {
            }
          }
        }

        if (hasMore) {
          moreLoading = false

          (repository.upstream as? HttpUpstream<*, *>)?.let { upstream ->
            if (!isCache && upstream.behavior == HttpUpstream.Behavior.Progressive) {
              if (page < INITIAL_PAGES) {
                moreLoading = true
                fetch(Repository.Origin.Network.origin, adapter.data.size)
              } else {
                initialFetched = true
              }
            }
          }
        }

        when (first) {
          true -> {
            adapter.notifyDataSetChanged()
            first = false
          }

          false -> adapter.notifyItemRangeInserted(adapter.data.size, data.size)
        }
      }
    }
  }
}
