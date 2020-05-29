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
import com.github.apognu.otter.utils.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.row_track.view.*

class SearchAdapter(private val context: Context?, private val favoriteListener: OnFavoriteListener? = null) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
  interface OnFavoriteListener {
    fun onToggleFavorite(id: Int, state: Boolean)
  }

  enum class ResultType {
    Header,
    Artist,
    Album,
    Track
  }

  val SECTION_COUNT = 3

  var artists: MutableList<Artist> = mutableListOf()
  var albums: MutableList<Album> = mutableListOf()
  var tracks: MutableList<Track> = mutableListOf()

  var currentTrack: Track? = null

  override fun getItemCount() = SECTION_COUNT + artists.size + albums.size + tracks.size

  override fun getItemId(position: Int): Long {
    return when (getItemViewType(position)) {
      ResultType.Header.ordinal -> {
        if (position == 0) return -1
        if (position == (artists.size + 1)) return -2
        return -3
      }

      ResultType.Artist.ordinal -> artists[position].id.toLong()
      ResultType.Artist.ordinal -> albums[position - artists.size - 2].id.toLong()
      ResultType.Track.ordinal -> tracks[position - artists.size - albums.size - SECTION_COUNT].id.toLong()
      else -> 0
    }
  }

  override fun getItemViewType(position: Int): Int {
    if (position == 0) return ResultType.Header.ordinal // Artists header
    if (position == (artists.size + 1)) return ResultType.Header.ordinal // Albums header
    if (position == (artists.size + albums.size + 2)) return ResultType.Header.ordinal // Tracks header

    if (position <= artists.size) return ResultType.Artist.ordinal
    if (position <= artists.size + albums.size + 2) return ResultType.Album.ordinal

    return ResultType.Track.ordinal
  }

  override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
    super.onAttachedToRecyclerView(recyclerView)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = when (viewType) {
      ResultType.Header.ordinal -> LayoutInflater.from(context).inflate(R.layout.row_search_header, parent, false)
      else -> LayoutInflater.from(context).inflate(R.layout.row_track, parent, false)
    }

    return ViewHolder(view, context).also {
      view.setOnClickListener(it)
    }
  }

  @SuppressLint("NewApi")
  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val resultType = getItemViewType(position)

    if (resultType == ResultType.Header.ordinal) {
      context?.let { context ->
        if (position == 0) holder.title.text = context.getString(R.string.artists)
        if (position == (artists.size + 1)) holder.title.text = context.getString(R.string.albums)
        if (position == (artists.size + albums.size + 2)) holder.title.text = context.getString(R.string.tracks)
      }

      return
    }

    val item = when (resultType) {
      ResultType.Artist.ordinal -> {
        holder.actions.visibility = View.GONE
        holder.favorite.visibility = View.GONE

        artists[position - 1]
      }

      ResultType.Album.ordinal -> {
        holder.actions.visibility = View.GONE
        holder.favorite.visibility = View.GONE

        albums[position - artists.size - 2]
      }

      ResultType.Track.ordinal -> tracks[position - artists.size - albums.size - SECTION_COUNT]

      else -> tracks[position]
    }

    Picasso.get()
      .maybeLoad(maybeNormalizeUrl(item.cover()))
      .fit()
      .transform(RoundedCornersTransformation(16, 0))
      .into(holder.cover)

    holder.title.text = item.title()
    holder.artist.text = item.subtitle()

    Build.VERSION_CODES.P.onApi(
      {
        holder.title.setTypeface(holder.title.typeface, Typeface.DEFAULT.weight)
        holder.artist.setTypeface(holder.artist.typeface, Typeface.DEFAULT.weight)
      },
      {
        holder.title.typeface = Typeface.create(holder.title.typeface, Typeface.NORMAL)
        holder.artist.typeface = Typeface.create(holder.artist.typeface, Typeface.NORMAL)
      })

    if (resultType == ResultType.Track.ordinal) {
      (item as? Track)?.let { track ->
        context?.let { context ->
          if (track == currentTrack || track.current) {
            holder.title.setTypeface(holder.title.typeface, Typeface.BOLD)
            holder.artist.setTypeface(holder.artist.typeface, Typeface.BOLD)
          }

          when (track.favorite) {
            true -> holder.favorite.setColorFilter(context.getColor(R.color.colorFavorite))
            false -> holder.favorite.setColorFilter(context.getColor(R.color.colorSelected))
          }

          holder.favorite.setOnClickListener {
            favoriteListener?.let {
              favoriteListener.onToggleFavorite(track.id, !track.favorite)

              tracks[position - artists.size - albums.size - SECTION_COUNT].favorite = !track.favorite

              notifyItemChanged(position)
            }
          }

          holder.actions.setOnClickListener {
            PopupMenu(context, holder.actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
              inflate(R.menu.row_track)

              setOnMenuItemClickListener {
                when (it.itemId) {
                  R.id.track_add_to_queue -> CommandBus.send(Command.AddToQueue(listOf(track)))
                  R.id.track_play_next -> CommandBus.send(Command.PlayNext(track))
                  R.id.queue_remove -> CommandBus.send(Command.RemoveFromQueue(track))
                }

                true
              }

              show()
            }
          }
        }
      }
    }
  }

  inner class ViewHolder(view: View, val context: Context?) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val handle = view.handle
    val cover = view.cover
    val title = view.title
    val artist = view.artist

    val favorite = view.favorite
    val actions = view.actions

    override fun onClick(view: View?) {
      when (getItemViewType(layoutPosition)) {
        ResultType.Track.ordinal -> {
          val position = layoutPosition - artists.size - albums.size - SECTION_COUNT

          tracks.subList(position, tracks.size).plus(tracks.subList(0, position)).apply {
            CommandBus.send(Command.ReplaceQueue(this))

            context.toast("All tracks were added to your queue")
          }
        }

        else -> {
        }
      }
    }
  }
}
