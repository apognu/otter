package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.Radio
import kotlinx.android.synthetic.main.row_radio.view.*

class RadiosAdapter(val context: Context?, private val listener: OnRadioClickListener) : FunkwhaleAdapter<Radio, RadiosAdapter.ViewHolder>() {
  interface OnRadioClickListener {
    fun onClick(holder: View?, radio: Radio)
  }

  override fun getItemCount() = data.size

  override fun getItemId(position: Int) = data[position].id.toLong()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadiosAdapter.ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_radio, parent, false)

    return ViewHolder(view, listener).also {
      view.setOnClickListener(it)
    }
  }

  override fun onBindViewHolder(holder: RadiosAdapter.ViewHolder, position: Int) {
    val radio = data[position]

    holder.art.visibility = View.VISIBLE
    holder.nativeArt.visibility = View.GONE
    holder.name.text = radio.name
    holder.description.text = radio.description

    context?.let { context ->
      val icon = when (radio.radio_type) {
        "random" -> R.drawable.shuffle
        "less-listened" -> R.drawable.sad
        else -> null
      }

      icon?.let {
        holder.art.visibility = View.GONE
        holder.nativeArt.visibility = View.VISIBLE

        holder.nativeArt.setImageDrawable(context.getDrawable(icon))
        holder.nativeArt.alpha = 0.7f
        holder.nativeArt.setColorFilter(context.getColor(R.color.controlForeground))
      }
    }
  }

  inner class ViewHolder(view: View, private val listener: OnRadioClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val nativeArt = view.native_art
    val art = view.art
    val name = view.name
    val description = view.description

    override fun onClick(view: View?) {
      listener.onClick(view, data[layoutPosition])
    }
  }
}