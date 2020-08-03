package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.models.domain.Artist
import com.github.apognu.otter.utils.maybeLoad
import com.github.apognu.otter.utils.maybeNormalizeUrl
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_artist.view.*

val DIFF = object : DiffUtil.ItemCallback<Artist>() {
  override fun areContentsTheSame(oldItem: Artist, newItem: Artist) = oldItem == newItem
  override fun areItemsTheSame(oldItem: Artist, newItem: Artist) = oldItem.id == newItem.id
}

class ArtistsAdapter(val context: Context?, private val listener: OnArtistClickListener) : PagingDataAdapter<Artist, ArtistsAdapter.ViewHolder>(DIFF) {
  interface OnArtistClickListener {
    fun onClick(holder: View?, artist: Artist)
  }

  // override fun getItemCount() = data.size

  // override fun getItemId(position: Int) = data[position].id.toLong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_artist, parent, false)

    return ViewHolder(view, listener).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val artist = getItem(position)!!

    Picasso.get()
      .maybeLoad(maybeNormalizeUrl(artist.album_cover))
      .fit()
      .transform(RoundedCornersTransformation(8, 0))
      .into(holder.art)

    holder.name.text = artist.name
    holder.albums.text = context?.resources?.getQuantityString(R.plurals.album_count, artist.album_count, artist.album_count) ?: ""
  }

  inner class ViewHolder(view: View, private val listener: OnArtistClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val art = view.art
    val name = view.name
    val albums = view.albums

    override fun onClick(view: View?) {
      getItem(layoutPosition)?.let { artist ->
        listener.onClick(view, artist)
      }
    }
  }
}