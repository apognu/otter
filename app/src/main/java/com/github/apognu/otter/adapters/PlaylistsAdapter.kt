package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.Playlist
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_playlist.view.*

class PlaylistsAdapter(val context: Context?, val listener: OnPlaylistClickListener) : FunkwhaleAdapter<Playlist, PlaylistsAdapter.ViewHolder>() {
  interface OnPlaylistClickListener {
    fun onClick(holder: View?, playlist: Playlist)
  }

  override fun getItemCount() = data.size

  override fun getItemId(position: Int) = data[position].id.toLong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_playlist, parent, false)

    return ViewHolder(view, listener).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val playlist = data[position]

    holder.name.text = playlist.name
    holder.summary.text = "${playlist.tracks_count} tracks • ${playlist.duration} seconds"

    playlist.album_covers.shuffled().take(4).forEachIndexed { index, url ->
      val imageView = when (index) {
        0 -> holder.cover_top_left
        1 -> holder.cover_top_right
        2 -> holder.cover_bottom_left
        3 -> holder.cover_bottom_right
        else -> holder.cover_top_left
      }

      Picasso.get()
        .load(url)
        .into(imageView)
    }
  }

  inner class ViewHolder(view: View, val listener: OnPlaylistClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val name = view.name
    val summary = view.summary

    val cover_top_left = view.cover_top_left
    val cover_top_right = view.cover_top_right
    val cover_bottom_left = view.cover_bottom_left
    val cover_bottom_right = view.cover_bottom_right

    override fun onClick(view: View?) {
      listener.onClick(view, data[layoutPosition])
    }
  }
}