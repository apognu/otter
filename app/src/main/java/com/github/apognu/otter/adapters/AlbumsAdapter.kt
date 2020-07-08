package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.maybeLoad
import com.github.apognu.otter.utils.maybeNormalizeUrl
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_album.view.*
import kotlinx.android.synthetic.main.row_artist.view.art

class AlbumsAdapter(val context: Context?, private val listener: OnAlbumClickListener) : FunkwhaleAdapter<Album, AlbumsAdapter.ViewHolder>() {
  interface OnAlbumClickListener {
    fun onClick(view: View?, album: Album)
  }

  override fun getItemCount() = data.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_album, parent, false)

    return ViewHolder(view, listener).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val album = data[position]

    Picasso.get()
      .maybeLoad(maybeNormalizeUrl(album.cover.original))
      .fit()
      .transform(RoundedCornersTransformation(8, 0))
      .into(holder.art)

    holder.title.text = album.title
    holder.artist.text = album.artist.name
    holder.release_date.visibility = View.GONE

    album.release_date.split('-').getOrNull(0)?.let { year ->
      if (year.isNotEmpty()) {
        holder.release_date.visibility = View.VISIBLE
        holder.release_date.text = year
      }
    }
  }

  inner class ViewHolder(view: View, private val listener: OnAlbumClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val art = view.art
    val title = view.title
    val artist = view.artist
    val release_date = view.release_date

    override fun onClick(view: View?) {
      listener.onClick(view, data[layoutPosition])
    }
  }
}