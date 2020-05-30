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

    holder.name.text = radio.name
    holder.description.text = radio.description
  }

  inner class ViewHolder(view: View, val listener: OnRadioClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val name = view.name
    val description = view.description

    override fun onClick(view: View?) {
      listener.onClick(view, data[layoutPosition])
    }
  }
}