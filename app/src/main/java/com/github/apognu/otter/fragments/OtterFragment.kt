package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.github.apognu.otter.repositories.HttpUpstream
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.log
import com.github.apognu.otter.utils.untilNetwork
import kotlinx.android.synthetic.main.fragment_artists.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import org.koin.ext.scope

abstract class OtterAdapter<D, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
  var data: MutableList<D> = mutableListOf()

  init {
    super.setHasStableIds(true)
  }

  abstract override fun getItemId(position: Int): Long
}

abstract class OtterFragment<DAO : Any, D : Any, A : OtterAdapter<D, *>> : Fragment() {
  open val OFFSCREEN_PAGES = 10

  abstract val repository: Repository<DAO>
  abstract val adapter: A
  open val viewModel: ViewModel? = null

  abstract val liveData: LiveData<List<D>>
  abstract val viewRes: Int
  abstract val recycler: RecyclerView

  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)
  open val alwaysRefresh = true

  private var moreLoading = false
  private var listener: Job? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    liveData.observe(viewLifecycleOwner) {
      onDataUpdated(it)

      adapter.data = it.toMutableList()
      adapter.notifyDataSetChanged()
    }

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
  }

  override fun onResume() {
    super.onResume()

    swiper?.setOnRefreshListener {
      fetch()
    }
  }

  open fun onDataFetched(data: List<DAO>) {}
  open fun onDataUpdated(data: List<D>?) {}

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

abstract class PagedOtterFragment<DAO : Any, D : Any, A : PagingDataAdapter<D, *>> : Fragment() {
  open val OFFSCREEN_PAGES = 10

  abstract val repository: Repository<DAO>
  abstract val adapter: A
  open val viewModel: ViewModel? = null

  abstract val liveData: LiveData<PagingData<D>>
  abstract val viewRes: Int
  abstract val recycler: RecyclerView

  open val layoutManager: RecyclerView.LayoutManager get() = LinearLayoutManager(context)
  open val alwaysRefresh = true

  private var moreLoading = false
  private var listener: Job? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(viewRes, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    liveData.observe(viewLifecycleOwner) {
      viewLifecycleOwner.lifecycleScope.launch(IO) {
        adapter.submitData(it)
      }
    }

    recycler.layoutManager = layoutManager
    recycler.adapter = adapter

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
  }

  open fun onDataFetched(data: List<DAO>) {}

  private fun fetch(size: Int = 0) {
    moreLoading = true

    repository.fetch(size).untilNetwork(lifecycleScope, IO) { data, _, hasMore ->
      lifecycleScope.launch(Main) {
        onDataFetched(data)

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
