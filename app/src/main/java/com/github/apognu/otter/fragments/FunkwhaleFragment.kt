package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.Cache
import com.github.apognu.otter.utils.log
import com.github.apognu.otter.utils.untilNetwork
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class FunkwhaleAdapter<D, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
  var data: MutableList<D> = mutableListOf()
}

abstract class FunkwhaleFragment<D : Any, A : FunkwhaleAdapter<D, *>> : Fragment() {
  abstract val viewRes: Int
  abstract val recycler: RecyclerView
  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)

  lateinit var repository: Repository<D, *>
  lateinit var adapter: A

  private var initialFetched = false

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
          if (recycler.computeVerticalScrollOffset() > 0 && !recycler.canScrollVertically(1)) {
            fetch(Repository.Origin.Network.origin, adapter.data.size)
          }
        }
      }
    }

    fetch(Repository.Origin.Cache.origin)

    if (adapter.data.isEmpty()) {
      fetch(Repository.Origin.Network.origin)
    }
  }

  override fun onResume() {
    super.onResume()

    swiper?.setOnRefreshListener {
      fetch(Repository.Origin.Network.origin)
    }
  }

  open fun onDataFetched(data: List<D>) {}

  private fun fetch(upstreams: Int = Repository.Origin.Network.origin, size: Int = 0) {
    var first = size == 0

    if (upstreams == Repository.Origin.Network.origin) {
      swiper?.isRefreshing = true
    }

    repository.fetch(upstreams, size).untilNetwork(IO) { data, isCache, hasMore ->
      GlobalScope.launch(Main) {
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

        if (!hasMore) {
          swiper?.isRefreshing = false

          GlobalScope.launch(IO) {
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
