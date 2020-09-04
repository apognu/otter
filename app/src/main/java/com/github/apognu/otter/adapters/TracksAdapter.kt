package com.github.apognu.otter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.OtterAdapter
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_track.view.*
import java.util.*

class TracksAdapter(private val context: Context?, private val favoriteListener: OnFavoriteListener? = null, val fromQueue: Boolean = false) : OtterAdapter<Track, TracksAdapter.ViewHolder>() {
  interface OnFavoriteListener {
    fun onToggleFavorite(id: Int, state: Boolean)
  }

  private lateinit var touchHelper: ItemTouchHelper

  var currentTrack: Track? = null

  override fun getItemId(position: Int): Long = data[position].id.toLong()

  override fun getItemCount() = data.size

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)

    if (fromQueue) {
      touchHelper = ItemTouchHelper(TouchHelperCallback()).also {
        it.attachToRecyclerView(recyclerView)
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.row_track, parent, false)

    return ViewHolder(view, context).also {
      view.setOnClickListener(it)
    }
  }

  @SuppressLint("NewApi")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val track = data[position]

    Picasso.get()
      .maybeLoad(maybeNormalizeUrl(track.album?.cover()))
      .fit()
      .transform(RoundedCornersTransformation(8, 0))
      .into(holder.cover)

    holder.title.text = track.title
    holder.artist.text = track.artist.name

    context?.let {
      holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.ripple)
    }

    if (track == currentTrack || track.current) {
      context?.let {
        holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.current)
      }
    }

    context?.let {
      when (track.favorite) {
        true -> holder.favorite.setColorFilter(context.getColor(R.color.colorFavorite))
        false -> holder.favorite.setColorFilter(context.getColor(R.color.colorSelected))
      }

      holder.favorite.setOnClickListener {
        favoriteListener?.let {
          favoriteListener.onToggleFavorite(track.id, !track.favorite)

          data[position].favorite = !track.favorite

          notifyItemChanged(position)
        }
      }

      when (track.cached || track.downloaded) {
        true -> holder.title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.downloaded, 0, 0, 0)
        false -> holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
      }

      if (track.cached && !track.downloaded) {
        holder.title.compoundDrawables.forEach {
          it?.colorFilter = PorterDuffColorFilter(context.getColor(R.color.cached), PorterDuff.Mode.SRC_IN)
        }
      }

      if (track.downloaded) {
        holder.title.compoundDrawables.forEach {
          it?.colorFilter = PorterDuffColorFilter(context.getColor(R.color.downloaded), PorterDuff.Mode.SRC_IN)
        }
      }
    }

    holder.actions.setOnClickListener {
      context?.let { context ->
        PopupMenu(context, holder.actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
          inflate(if (fromQueue) R.menu.row_queue else R.menu.row_track)

          setOnMenuItemClickListener {
            when (it.itemId) {
              R.id.track_add_to_queue -> CommandBus.send(Command.AddToQueue(listOf(track)))
              R.id.track_play_next -> CommandBus.send(Command.PlayNext(track))
              R.id.track_pin -> CommandBus.send(Command.PinTrack(track))
              R.id.track_add_to_playlist -> CommandBus.send(Command.AddToPlaylist(track))
              R.id.queue_remove -> CommandBus.send(Command.RemoveFromQueue(track))
            }

            true
          }

          show()
        }
      }
    }

    if (fromQueue) {
      holder.handle.visibility = View.VISIBLE

      holder.handle.setOnTouchListener { _, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          touchHelper.startDrag(holder)
        }

        true
      }
    }
  }

  fun onItemMove(oldPosition: Int, newPosition: Int) {
    if (oldPosition < newPosition) {
      for (i in oldPosition.until(newPosition)) {
        Collections.swap(data, i, i + 1)
      }
    } else {
      for (i in oldPosition.downTo(newPosition + 1)) {
        Collections.swap(data, i, i - 1)
      }
    }

    notifyItemMoved(oldPosition, newPosition)
  }

  inner class ViewHolder(view: View, val context: Context?) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val handle = view.handle
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

  inner class TouchHelperCallback : ItemTouchHelper.Callback() {
    var from = -1
    var to = -1

    override fun isLongPressDragEnabled() = false

    override fun isItemViewSwipeEnabled() = false

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) =
      makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
      to = target.adapterPosition

      onItemMove(viewHolder.adapterPosition, to)

      return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
      if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
        context?.let {
          viewHolder?.let {
            from = viewHolder.adapterPosition
            viewHolder.itemView.background = ColorDrawable(context.getColor(R.color.colorSelected))
          }
        }
      }

      super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
      if (from != -1 && to != -1) {
        CommandBus.send(Command.MoveFromQueue(from, to))

        from = -1
        to = -1
      }

      viewHolder.itemView.background = ColorDrawable(Color.TRANSPARENT)

      super.clearView(recyclerView, viewHolder)
    }
  }
}
