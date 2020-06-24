package com.github.apognu.otter.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.AppContext
import com.github.apognu.otter.utils.Event
import com.github.apognu.otter.utils.EventBus
import com.github.apognu.otter.utils.Radio
import com.github.apognu.otter.views.LoadingImageView
import com.preference.PowerPreference
import kotlinx.android.synthetic.main.row_radio.view.*
import kotlinx.android.synthetic.main.row_radio_header.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RadiosAdapter(val context: Context?, val scope: CoroutineScope, private val listener: OnRadioClickListener) : FunkwhaleAdapter<Radio, RadiosAdapter.ViewHolder>() {
  interface OnRadioClickListener {
    fun onClick(holder: ViewHolder, radio: Radio)
  }

  enum class RowType {
    Header,
    InstanceRadio,
    UserRadio
  }

  private val instanceRadios: List<Radio> by lazy {
    context?.let {
      return@lazy when (val username = PowerPreference.getFileByName(AppContext.PREFS_CREDENTIALS).getString("actor_username")) {
        "" -> listOf(
          Radio(0, "random", context.getString(R.string.radio_random_title), context.getString(R.string.radio_random_description))
        )

        else -> listOf(
          Radio(0, "actor_content", context.getString(R.string.radio_your_content_title), context.getString(R.string.radio_your_content_description), username),
          Radio(0, "random", context.getString(R.string.radio_random_title), context.getString(R.string.radio_random_description)),
          Radio(0, "favorites", context.getString(R.string.favorites), context.getString(R.string.radio_favorites_description)),
          Radio(0, "less-listened", context.getString(R.string.radio_less_listened_title), context.getString(R.string.radio_less_listened_description))
        )
      }
    }

    listOf<Radio>()
  }

  private fun getRadioAt(position: Int): Radio {
    return when (getItemViewType(position)) {
      RowType.InstanceRadio.ordinal -> instanceRadios[position - 1]
      else -> data[position - instanceRadios.size - 2]
    }
  }

  override fun getItemCount() = instanceRadios.size + data.size + 2

  override fun getItemId(position: Int) = data[position].id.toLong()

  override fun getItemViewType(position: Int): Int {
    return when {
      position == 0 || position == instanceRadios.size + 1 -> RowType.Header.ordinal
      position <= instanceRadios.size -> RowType.InstanceRadio.ordinal
      else -> RowType.UserRadio.ordinal
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadiosAdapter.ViewHolder {
    return when (viewType) {
      RowType.InstanceRadio.ordinal, RowType.UserRadio.ordinal -> {
        val view = LayoutInflater.from(context).inflate(R.layout.row_radio, parent, false)

        ViewHolder(view, listener).also {
          view.setOnClickListener(it)
        }
      }

      else -> ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_radio_header, parent, false), null)
    }
  }

  override fun onBindViewHolder(holder: RadiosAdapter.ViewHolder, position: Int) {
    when (getItemViewType(position)) {
      RowType.Header.ordinal -> {
        context?.let {
          when (position) {
            0 -> holder.label.text = context.getString(R.string.radio_instance_radios)
            instanceRadios.size + 1 -> holder.label.text = context.getString(R.string.radio_user_radios)
          }
        }
      }

      RowType.InstanceRadio.ordinal, RowType.UserRadio.ordinal -> {
        val radio = getRadioAt(position)

        holder.art.visibility = View.VISIBLE
        holder.name.text = radio.name
        holder.description.text = radio.description

        context?.let { context ->
          val icon = when (radio.radio_type) {
            "actor_content" -> R.drawable.library
            "favorites" -> R.drawable.favorite
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
    }
  }

  inner class ViewHolder(view: View, private val listener: OnRadioClickListener?) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val label = view.label
    val art = view.art
    val name = view.name
    val description = view.description

    var native = false

    override fun onClick(view: View?) {
      listener?.onClick(this, getRadioAt(layoutPosition))
    }

    fun spin() {
      context?.let {
        val originalDrawable = art.drawable
        val originalColorFilter = art.colorFilter
        val imageAnimator = LoadingImageView.start(context, art)

        art.setColorFilter(context.getColor(R.color.controlForeground))

        scope.launch(Main) {
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