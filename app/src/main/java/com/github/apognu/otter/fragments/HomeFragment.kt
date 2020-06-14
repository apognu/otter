package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.home.DummyAdapter
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.repositories.home.RecentlyAddedRepository
import com.github.apognu.otter.repositories.home.RecentlyListenedRepository
import com.github.apognu.otter.repositories.home.TagsRepository
import com.github.apognu.otter.utils.untilNetwork
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
  private lateinit var tagsRepository: TagsRepository
  private lateinit var recentlyAddedRepository: RecentlyAddedRepository
  private lateinit var recentlyListenedRepository: RecentlyListenedRepository

  private lateinit var tagsAdapter: DummyAdapter
  private lateinit var recentlyAddedAdapter: DummyAdapter
  private lateinit var recentlyListenedAdapter: DummyAdapter
  private lateinit var dummyAdapter: DummyAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    tagsRepository = TagsRepository(context)
    recentlyAddedRepository = RecentlyAddedRepository(context)
    recentlyListenedRepository = RecentlyListenedRepository(context)

    tagsAdapter = DummyAdapter(context, R.layout.row_tag)
    recentlyAddedAdapter = DummyAdapter(context)
    recentlyListenedAdapter = DummyAdapter(context)
    dummyAdapter = DummyAdapter(context)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_home, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    tags.apply {
      adapter = tagsAdapter
      layoutManager = FlexboxLayoutManager(context).apply {
        isNestedScrollingEnabled = false
      }
    }

    random.apply {
      adapter = dummyAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    recently_listened.apply {
      adapter = recentlyListenedAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    recently_added.apply {
      adapter = recentlyAddedAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    playlists.apply {
      adapter = dummyAdapter
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    refresh()
  }

  private fun refresh() {
    tagsRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) {data, _, _ ->
      GlobalScope.launch(Main) {
        tagsAdapter.data = data.map { DummyAdapter.DummyItem(it.name, null) }
        tagsAdapter.notifyDataSetChanged()
      }
    }

    recentlyListenedRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) { data, _, _ ->
      GlobalScope.launch(Main) {
        recentlyListenedAdapter.data = data.map { DummyAdapter.DummyItem(it.track.title, it.track.album.cover.original) }
        recentlyListenedAdapter.notifyDataSetChanged()
      }
    }

    recentlyAddedRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) { data, _, _ ->
      GlobalScope.launch(Main) {
        recentlyAddedAdapter.data = data.map { DummyAdapter.DummyItem(it.title, it.album.cover.original) }
        recentlyAddedAdapter.notifyDataSetChanged()
      }
    }
  }
}