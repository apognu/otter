package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.Radio
import com.github.apognu.otter.views.LoadingImageView
import kotlinx.android.synthetic.main.row_radio.view.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RadiosAdapter(val context: Context?, private val listener: OnRadioClickListener) : FunkwhaleAdapter<Radio, RadiosAdapter.ViewHolder>() {
  interface OnRadioClickListener {
    fun onClick(holder: ViewHolder, radio: Radio)
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
    holder.name.text = radio.name
    holder.description.text = radio.description

    context?.let { context ->
      val icon = when (radio.radio_type) {
        "random" -> R.drawable.shuffle
        "less-listened" -> R.drawable.sad
        else -> null
      }

      icon?.let {
        holder.native = true

        holder.art.setImageDrawable(context.getDrawable(icon))
        holder.art.alpha = 0.7f
        holder.art.setColorFilter(context.getColor(R.color.controlForeground))
      }
    }
  }

  inner class ViewHolder(view: View, private val listener: OnRadioClickListener) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val art = view.art
    val name = view.name
    val description = view.description

    var native = false

    override fun onClick(view: View?) {
      listener.onClick(this, data[layoutPosition])
    }

    fun spin() {
      context?.let {
        val originalDrawable = art.drawable
        val originalColorFilter = art.colorFilter
        val imageAnimator = LoadingImageView.start(context, art)

        art.setColorFilter(context.getColor(R.color.controlForeground))

        GlobalScope.launch(Main) {
          EventBus.get().collect { message ->
            when (message) {
              is Event.RadioStarted -> {
                art.colorFilter = originalColorFilter

                LoadingImageView.stop(context, originalDrawable, art, imageAnimator)
              }
            }
          }
        }
      }
    }
  }
}