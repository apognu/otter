package com.github.apognu.otter.adapters.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.HomeFragment
import com.github.apognu.otter.utils.Artist
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_home_media.view.*

class HomeMediaAdapter(
  private val context: Context?,
  private val kind: ItemType,
  private val viewRes: Int = R.layout.row_home_media,
  private val listener: HomeFragment.OnHomeClickListener? = null
) : RecyclerView.Adapter<HomeMediaAdapter.ViewHolder>() {

  enum class ItemType {
    Tag, Artist, Album, Track
  }

  data class HomeMediaItem(
    val label: String,
    val cover: String?,
    val artist: Artist? = null
  )

  var data: List<HomeMediaItem> = listOf()

  override fun getItemCount() = data.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(viewRes, parent, false)

    return ViewHolder(view).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    data[position].also {
      holder.label.text = it.label

      it.cover?.let { cover ->
        Picasso
          .get()
          .load(mustNormalizeUrl(cover))
          .fit()
          .placeholder(R.drawable.cover).placeholder(R.drawable.cover)
          .transform(RoundedCornersTransformation(16, 0))
          .into(holder.cover)
      }
    }
  }

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val label = view.label
    val cover = view.cover

    override fun onClick(view: View?) {
      when {
        kind == ItemType.Artist -> listener?.onClick(artist = data[layoutPosition].artist)
      }
    }
  }
}