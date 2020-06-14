package com.github.apognu.otter.adapters.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_dummy.view.*

class DummyAdapter(val context: Context?, val viewRes: Int = R.layout.row_dummy) : RecyclerView.Adapter<DummyAdapter.ViewHolder>() {
  data class DummyItem(val label: String, val cover: String?)

  var data: List<DummyItem> = listOf()

  override fun getItemCount() = data.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return LayoutInflater.from(context).inflate(viewRes, parent, false).run {
      ViewHolder(this)
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

  inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val label = view.label
    val cover = view.cover
  }
}