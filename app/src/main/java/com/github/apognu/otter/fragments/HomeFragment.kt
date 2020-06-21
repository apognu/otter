package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.home.HomeMediaAdapter
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

  private lateinit var tagsAdapter: HomeMediaAdapter
  private lateinit var recentlyAddedAdapter: HomeMediaAdapter
  private lateinit var recentlyListenedAdapter: HomeMediaAdapter
  private lateinit var randomAdapter: HomeMediaAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    tagsRepository = TagsRepository(context)
    recentlyAddedRepository = RecentlyAddedRepository(context)
    recentlyListenedRepository = RecentlyListenedRepository(context)

    tagsAdapter = HomeMediaAdapter(context, R.layout.row_tag)
    recentlyAddedAdapter = HomeMediaAdapter(context)
    recentlyListenedAdapter = HomeMediaAdapter(context)
    randomAdapter = HomeMediaAdapter(context)
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
      adapter = randomAdapter
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

    refresh()
  }

  private fun refresh() {
    tagsRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) {data, _, _ ->
      GlobalScope.launch(Main) {
        tagsAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.name, null) }
        tagsAdapter.notifyDataSetChanged()

        tags_loader.visibility = View.GONE
        tags.visibility = View.VISIBLE
      }
    }

    recentlyListenedRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) { data, _, _ ->
      GlobalScope.launch(Main) {
        recentlyListenedAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.track.title, it.track.album.cover.original) }
        recentlyListenedAdapter.notifyDataSetChanged()

        recently_listened_loader.visibility = View.GONE
        recently_listened.visibility = View.VISIBLE
      }
    }

    recentlyAddedRepository.fetch(Repository.Origin.Network.origin).untilNetwork(IO) { data, _, _ ->
      GlobalScope.launch(Main) {
        recentlyAddedAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.title, it.album.cover.original) }
        recentlyAddedAdapter.notifyDataSetChanged()

        recently_added_loader.visibility = View.GONE
        recently_added.visibility = View.VISIBLE
      }
    }
  }
}