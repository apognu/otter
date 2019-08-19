package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.Track
import kotlinx.android.synthetic.main.row_track.view.*

class SearchResultsAdapter(val context: Context?) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {
  var tracks: List<Track> = listOf()

  override fun getItemCount() = tracks.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_track, parent, false)

    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val artist = tracks[position]

    holder.title.text = artist.title
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title = view.title
  }
}