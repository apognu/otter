package com.github.apognu.otter.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.*

class BrowseTabsAdapter(val context: Fragment, manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
  var tabs = mutableListOf<Fragment>()

  override fun getCount() = 6

  override fun getItem(position: Int): Fragment {
    tabs.getOrNull(position)?.let {
      return it
    }

    val fragment = when (position) {
      0 -> HomeFragment()
      1 -> ArtistsFragment()
      2 -> AlbumsGridFragment()
      3 -> PlaylistsFragment()
      4 -> RadiosFragment()
      5 -> FavoritesFragment()
      else -> ArtistsFragment()
    }

    tabs.add(position, fragment)

    return fragment
  }

  override fun getPageTitle(position: Int): String {
    return when (position) {
      0 -> "Otter"
      1 -> context.getString(R.string.artists)
      2 -> context.getString(R.string.albums)
      3 -> context.getString(R.string.playlists)
      4 -> context.getString(R.string.radios)
      5 -> context.getString(R.string.favorites)
      else -> ""
    }
  }
}