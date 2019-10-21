package com.github.apognu.otter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.FunkwhaleAdapter
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_track.view.*
import java.util.*

class FavoritesAdapter(private val context: Context?, private val favoriteListener: OnFavoriteListener, val fromQueue: Boolean = false) : FunkwhaleAdapter<Favorite, FavoritesAdapter.ViewHolder>() {
  interface OnFavoriteListener {
    fun onToggleFavorite(id: Int, state: Boolean)
  }

  var currentTrack: Track? = null

  override fun getItemCount() = data.size

  override fun getItemId(position: Int): Long {
    return data[position].track.id.toLong()
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
      .load(normalizeUrl(favorite.track.album.cover.original))
      .fit()
      .placeholder(R.drawable.cover)
      .transform(RoundedCornersTransformation(16, 0))
      .into(holder.cover)

    holder.title.text = favorite.track.title
    holder.artist.text = favorite.track.artist.name

    Build.VERSION_CODES.P.onApi(
      {
        holder.title.setTypeface(holder.title.typeface, Typeface.DEFAULT.weight)
        holder.artist.setTypeface(holder.artist.typeface, Typeface.DEFAULT.weight)
      },
      {
        holder.title.setTypeface(holder.title.typeface, Typeface.NORMAL)
        holder.artist.setTypeface(holder.artist.typeface, Typeface.NORMAL)
      })


    if (favorite.track == currentTrack || favorite.track.current) {
      holder.title.setTypeface(holder.title.typeface, Typeface.BOLD)
      holder.artist.setTypeface(holder.artist.typeface, Typeface.BOLD)
    }

    context?.let {
      when (favorite.track.favorite) {
        true -> holder.favorite.setColorFilter(context.resources.getColor(R.color.colorFavorite))
        false -> holder.favorite.setColorFilter(context.resources.getColor(R.color.colorSelected))
      }

      holder.favorite.setOnClickListener {
        favoriteListener.onToggleFavorite(favorite.track.id, !favorite.track.favorite)

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
              R.id.track_add_to_queue -> CommandBus.send(Command.AddToQueue(listOf(favorite.track)))
              R.id.track_play_next -> CommandBus.send(Command.PlayNext(favorite.track))
              R.id.queue_remove -> CommandBus.send(Command.RemoveFromQueue(favorite.track))
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
            CommandBus.send(Command.ReplaceQueue(this.map { it.track }))

            context.toast("All tracks were added to your queue")
          }
        }
      }
    }
  }
}
