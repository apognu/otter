package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.OtterAdapter
import com.github.apognu.otter.utils.Playlist
import com.github.apognu.otter.utils.toDurationString
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_playlist.view.*

class PlaylistsAdapter(val context: Context?, private val listener: OnPlaylistClickListener) : OtterAdapter<Playlist, PlaylistsAdapter.ViewHolder>() {
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
    holder.summary.text = context?.resources?.getQuantityString(R.plurals.playlist_description, playlist.tracks_count, playlist.tracks_count, toDurationString(playlist.duration.toLong())) ?: ""

    context?.let {
      ContextCompat.getDrawable(context, R.drawable.cover).let {
        holder.cover_top_left.setImageDrawable(it)
        holder.cover_top_right.setImageDrawable(it)
        holder.cover_bottom_left.setImageDrawable(it)
        holder.cover_bottom_right.setImageDrawable(it)
      }
    }

    playlist.album_covers.shuffled().take(4).forEachIndexed { index, url ->
      val imageView = when (index) {
        0 -> holder.cover_top_left
        1 -> holder.cover_top_right
        2 -> holder.cover_bottom_left
        3 -> holder.cover_bottom_right
        else -> holder.cover_top_left
      }

      val corner = when (index) {
        0 -> RoundedCornersTransformation.CornerType.TOP_LEFT
        1 -> RoundedCornersTransformation.CornerType.TOP_RIGHT
        2 -> RoundedCornersTransformation.CornerType.BOTTOM_LEFT
        3 -> RoundedCornersTransformation.CornerType.BOTTOM_RIGHT
        else -> RoundedCornersTransformation.CornerType.TOP_LEFT
      }

      Picasso.get()
        .load(url)
        .transform(RoundedCornersTransformation(32, 0, corner))
        .into(imageView)
    }
  }

  inner class ViewHolder(view: View, private val listener: OnPlaylistClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
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