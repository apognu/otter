package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.fragment_artists.*

abstract class FunkwhaleAdapter<D, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
  var data: MutableList<D> = mutableListOf()
}

abstract class FunkwhaleFragment<D : Any, A : FunkwhaleAdapter<D, *>> : Fragment() {
  abstract val viewRes: Int
  abstract val recycler: RecyclerView
  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)

  lateinit var repository: Repository<D, *>
  lateinit var adapter: A

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler.layoutManager = layoutManager
    recycler.adapter = adapter

    scroller?.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
      if (!scroller.canScrollVertically(1)) {
        repository.fetch(Repository.Origin.Network.origin, adapter.data).untilNetwork {
          swiper?.isRefreshing = false

          onDataFetched(it)

          adapter.data = it.toMutableList()
          adapter.notifyDataSetChanged()
        }
      }
    }

    swiper?.isRefreshing = true

    repository.fetch().untilNetwork {
      swiper?.isRefreshing = false

      onDataFetched(it)

      adapter.data = it.toMutableList()
      adapter.notifyDataSetChanged()
    }
  }

  override fun onResume() {
    super.onResume()

    recycler.adapter = adapter

    swiper?.setOnRefreshListener {
      repository.fetch(Repository.Origin.Network.origin, listOf()).untilNetwork {
        swiper?.isRefreshing = false

        onDataFetched(it)

        adapter.data = it.toMutableList()
        adapter.notifyDataSetChanged()
      }
    }
  }

  open fun onDataFetched(data: List<D>) {}
}
