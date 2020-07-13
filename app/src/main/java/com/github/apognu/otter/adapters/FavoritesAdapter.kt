package com.github.apognu.otter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.OtterAdapter
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.models.domain.Track
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_track.view.*
import java.util.*

class FavoritesAdapter(private val context: Context?, private val favoriteListener: OnFavoriteListener, val fromQueue: Boolean = false) : OtterAdapter<Track, FavoritesAdapter.ViewHolder>() {
  interface OnFavoriteListener {
    fun onToggleFavorite(id: Int, state: Boolean)
  }

  var currentTrack: Track? = null

  override fun getItemCount() = data.size

  override fun getItemId(position: Int): Long {
    return data[position].id.toLong()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_track, parent, false)

    return ViewHolder(view, context).also {
      view.setOnClickListener(it)
    }
  }

  @SuppressLint("NewApi")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val favorite = data[position]

    Picasso.get()
      .maybeLoad(maybeNormalizeUrl(favorite.album?.cover()))
      .fit()
      .placeholder(R.drawable.cover)
      .transform(RoundedCornersTransformation(16, 0))
      .into(holder.cover)

    holder.title.text = favorite.title
    holder.artist.text = favorite.artist?.name

    context?.let {
      holder.itemView.background = context.getDrawable(R.drawable.ripple)
    }

    if (favorite.id == currentTrack?.id) {
      context?.let {
        holder.itemView.background = context.getDrawable(R.drawable.current)
      }
    }

    context?.let {
      when (favorite.favorite) {
        true -> holder.favorite.setColorFilter(context.getColor(R.color.colorFavorite))
        false -> holder.favorite.setColorFilter(context.getColor(R.color.colorSelected))
      }

      when (favorite.cached || favorite.downloaded) {
        true -> holder.title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.downloaded, 0, 0, 0)
        false -> holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
      }

      if (favorite.cached && !favorite.downloaded) {
        holder.title.compoundDrawables.forEach {
          it?.colorFilter = PorterDuffColorFilter(context.getColor(R.color.cached), PorterDuff.Mode.SRC_IN)
        }
      }

      if (favorite.downloaded) {
        holder.title.compoundDrawables.forEach {
          it?.colorFilter = PorterDuffColorFilter(context.getColor(R.color.downloaded), PorterDuff.Mode.SRC_IN)
        }
      }

      holder.favorite.setOnClickListener {
        favoriteListener.onToggleFavorite(favorite.id, !favorite.favorite)

        data.remove(favorite)
        notifyItemRemoved(holder.adapterPosition)
      }
    }

    holder.actions.setOnClickListener {
      context?.let { context ->
        PopupMenu(context, holder.actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
          inflate(if (fromQueue) R.menu.row_queue else R.menu.row_track)

          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.track_add_to_queue -> CommandBus.send(Command.AddToQueue(listOf(favorite)))
              R.id.track_play_next -> CommandBus.send(Command.PlayNext(favorite))
              R.id.track_pin -> CommandBus.send(Command.PinTrack(favorite))
              R.id.queue_remove -> CommandBus.send(Command.RemoveFromQueue(favorite))
            }

            true
          }

          show()
        }
      }
    }
  }

  fun onItemMove(oldPosition: Int, newPosition: Int) {
    if (oldPosition < newPosition) {
      for (i in oldPosition.until(newPosition)) {
        Collections.swap(data, i, i + 1)
      }
    } else {
      for (i in newPosition.downTo(oldPosition)) {
        Collections.swap(data, i, i - 1)
      }
    }

    notifyItemMoved(oldPosition, newPosition)
    CommandBus.send(Command.MoveFromQueue(oldPosition, newPosition))
  }

  inner class ViewHolder(view: View, val context: Context?) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val cover = view.cover
    val title = view.title
    val artist = view.artist

    val favorite = view.favorite
    val actions = view.actions

    override fun onClick(view: View?) {
      when (fromQueue) {
        true -> CommandBus.send(Command.PlayTrack(layoutPosition))
        false -> {
          data.subList(layoutPosition, data.size).plus(data.subList(0, layoutPosition)).apply {
            CommandBus.send(Command.ReplaceQueue(this))

            context.toast("All tracks were added to your queue")
          }
        }
      }
    }
  }
}
