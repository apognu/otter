package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.Album
import com.github.apognu.otter.utils.normalizeUrl
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_album_grid.view.*

class AlbumsGridAdapter(val context: Context?, val listener: OnAlbumClickListener) : FunkwhaleAdapter<Album, AlbumsGridAdapter.ViewHolder>() {
  interface OnAlbumClickListener {
    fun onClick(view: View?, album: Album)
  }

  override fun getItemCount() = data.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_album_grid, parent, false)

    return ViewHolder(view, listener).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val album = data[position]

    Picasso.get()
      .load(normalizeUrl(album.cover.original))
      .fit()
      .placeholder(R.drawable.cover)
      .transform(RoundedCornersTransformation(24, 0))
      .into(holder.cover)

    holder.title.text = album.title
  }

  inner class ViewHolder(view: View, val listener: OnAlbumClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val cover = view.cover
    val title = view.title

    override fun onClick(view: View?) {
      listener.onClick(view, data[layoutPosition])
    }
  }
}