package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.home.HomeMediaAdapter
import com.github.apognu.otter.adapters.home.HomeMediaAdapter.ItemType
import com.github.apognu.otter.repositories.Repository
import com.github.apognu.otter.repositories.home.RandomArtistsRepository
import com.github.apognu.otter.repositories.home.RecentlyAddedRepository
import com.github.apognu.otter.repositories.home.RecentlyListenedRepository
import com.github.apognu.otter.repositories.home.TagsRepository
import com.github.apognu.otter.utils.*
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class HomeFragment : Fragment() {
  interface OnHomeClickListener {
    fun onClick(artist: Artist? = null, album: Album? = null, track: Track? = null)
  }

  val CACHE_DURATION = 15 * 60 * 1000

  private var bus: Job? = null

  private lateinit var tagsRepository: TagsRepository
  private lateinit var randomArtistsRepository: RandomArtistsRepository
  private lateinit var recentlyAddedRepository: RecentlyAddedRepository
  private lateinit var recentlyListenedRepository: RecentlyListenedRepository

  private lateinit var tagsAdapter: HomeMediaAdapter
  private lateinit var randomAdapter: HomeMediaAdapter
  private lateinit var recentlyAddedAdapter: HomeMediaAdapter
  private lateinit var recentlyListenedAdapter: HomeMediaAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    tagsRepository = TagsRepository(context)
    randomArtistsRepository = RandomArtistsRepository(context)
    recentlyAddedRepository = RecentlyAddedRepository(context)
    recentlyListenedRepository = RecentlyListenedRepository(context)

    tagsAdapter = HomeMediaAdapter(context, ItemType.Tag, R.layout.row_tag)
    randomAdapter = HomeMediaAdapter(context, ItemType.Artist, listener = ArtistClickListener())
    recentlyAddedAdapter = HomeMediaAdapter(context, ItemType.Track)
    recentlyListenedAdapter = HomeMediaAdapter(context, ItemType.Track)
  }

  override fun onResume() {
    super.onResume()

    bus = GlobalScope.launch(IO) {
      EventBus.get().collect { event ->
        if (event is Event.ListingsChanged) {
          refresh(true)
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()

    bus?.cancel()
    bus = null
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_home, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    tags.apply {
      isNestedScrollingEnabled = false

      adapter = tagsAdapter
      layoutManager = FlexboxLayoutManager(context).apply {
        justifyContent = JustifyContent.SPACE_BETWEEN
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

  private fun originFor(repository: Repository<*, *>, force: Boolean = false): Repository.Origin {
    if (force) return Repository.Origin.Network

    repository.cacheId?.let { cacheId ->
      repository.cache(listOf())?.let {
        Cache.get(context, "$cacheId-at")?.readLine()?.toLong()?.let { date ->
          return if ((Date().time - date) < CACHE_DURATION) Repository.Origin.Cache
          else Repository.Origin.Network
        }
      }
    }

    return Repository.Origin.Network
  }

  private fun <T: Any> cache(repository: Repository<T, *>, data: List<T>) {
    repository.cacheId?.let { cacheId ->
      repository.cache(data)?.let { cache ->
        Cache.set(
          context,
          cacheId,
          Gson().toJson(cache).toByteArray()
        )

        Cache.set(context, "$cacheId-at", Date().time.toString().toByteArray())
      }
    }
  }

  private fun refresh(force: Boolean = false) {
    tagsRepository.fetch(originFor(tagsRepository, force).origin).untilNetwork(IO) { data, isCache, _ ->
      GlobalScope.launch(Main) {
        tagsAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.name, null) }
        tagsAdapter.notifyDataSetChanged()

        tags_loader?.visibility = View.GONE
        tags?.visibility = View.VISIBLE

        if (!isCache) cache(tagsRepository, data)
      }
    }

    randomArtistsRepository.fetch(originFor(randomArtistsRepository, force).origin).untilNetwork(IO) { data, isCache, _ ->
      GlobalScope.launch(Main) {
        randomAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.name, it.albums?.getOrNull(0)?.cover?.original, artist = it) }
        randomAdapter.notifyDataSetChanged()

        random_loader?.visibility = View.GONE
        random?.visibility = View.VISIBLE

        if (!isCache) cache(randomArtistsRepository, data)
      }
    }

    recentlyListenedRepository.fetch(originFor(recentlyListenedRepository, force).origin).untilNetwork(IO) { data, isCache, _ ->
      GlobalScope.launch(Main) {
        recentlyListenedAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.track.title, it.track.album.cover.original) }
        recentlyListenedAdapter.notifyDataSetChanged()

        recently_listened_loader?.visibility = View.GONE
        recently_listened?.visibility = View.VISIBLE

        if (!isCache) cache(recentlyListenedRepository, data)
      }
    }

    recentlyAddedRepository.fetch(originFor(recentlyAddedRepository, force).origin).untilNetwork(IO) { data, isCache, _ ->
      GlobalScope.launch(Main) {
        recentlyAddedAdapter.data = data.map { HomeMediaAdapter.HomeMediaItem(it.title, it.album.cover.original) }
        recentlyAddedAdapter.notifyDataSetChanged()

        recently_added_loader?.visibility = View.GONE
        recently_added?.visibility = View.VISIBLE

        if (!isCache) cache(recentlyAddedRepository, data)
      }
    }
  }

  inner class ArtistClickListener : OnHomeClickListener {
    override fun onClick(artist: Artist?, album: Album?, track: Track?) {
      artist?.let {
        ArtistsFragment.openAlbums(context, artist)
      }
    }
  }
}