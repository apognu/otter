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
          if (!recycler.canScrollVertically(1)) {
            fetch(Repository.Origin.Network.origin, adapter.data.size)
          }
        }
      }
    }

    fetch()
  }

  override fun onResume() {
    super.onResume()

    swiper?.setOnRefreshListener {
      fetch(Repository.Origin.Network.origin)
    }
  }

  open fun onDataFetched(data: List<D>) {}

  private fun fetch(upstreams: Int = (Repository.Origin.Network.origin and Repository.Origin.Cache.origin), size: Int = 0) {
    var cleared = false

    swiper?.isRefreshing = true

    if (size == 0) {
      cleared = true
      adapter.data.clear()
    }

    repository.fetch(upstreams, size).untilNetwork(IO) { data, hasMore ->
      onDataFetched(data)

      if (!hasMore) {
        swiper?.isRefreshing = false

        repository.cacheId?.let { cacheId ->
          Cache.set(
            context,
            cacheId,
            Gson().toJson(repository.cache(adapter.data)).toByteArray()
          )
        }
      }

      GlobalScope.launch(Main) {
        adapter.data.addAll(data)

        when (cleared) {
          true -> {
            adapter.notifyDataSetChanged()
            cleared = false
          }
          false -> adapter.notifyItemRangeInserted(adapter.data.size, data.size)
        }
      }
    }
  }
}
