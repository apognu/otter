package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.BrowseTabsAdapter
import kotlinx.android.synthetic.main.fragment_browse.view.*

class BrowseFragment : Fragment() {
  var adapter: BrowseTabsAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = BrowseTabsAdapter(this, childFragmentManager)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_browse, container, false).apply {
      tabs.setupWithViewPager(pager)
      tabs.getTabAt(0)?.select()

      pager.adapter = adapter
      pager.offscreenPageLimit = 4
    }
  }

  fun selectTabAt(position: Int) {
    view?.tabs?.getTabAt(position)?.select()
  }
}