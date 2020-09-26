package com.github.apognu.otter.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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

class SearchAdapter(private val context: Context?, private val listener: OnSearchResultClickListener? = null, private val favoriteListener: OnFavoriteListener? = null) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
  interface OnSearchResultClickListener {
    fun onArtistClick(holder: View?, artist: Artist)
    fun onAlbumClick(holder: View?, album: Album)
  }

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
        if (position == 0) {
          holder.title.text = context.getString(R.string.artists)
          holder.itemView.visibility = View.VISIBLE
          holder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

          if (artists.isEmpty()) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
          }
        }

        if (position == (artists.size + 1)) {
          holder.title.text = context.getString(R.string.albums)
          holder.itemView.visibility = View.VISIBLE
          holder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

          if (albums.isEmpty()) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
          }
        }

        if (position == (artists.size + albums.size + 2)) {
          holder.title.text = context.getString(R.string.tracks)
          holder.itemView.visibility = View.VISIBLE
          holder.itemView.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

          if (tracks.isEmpty()) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
          }
        }
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

    holder.title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)

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

          holder.actions.setOnClickListener {
            PopupMenu(context, holder.actions, Gravity.START, R.attr.actionOverflowMenuStyle, 0).apply {
              inflate(R.menu.row_track)

              setOnMenuItemClickListener {
                when (it.itemId) {
                  R.id.track_add_to_queue -> CommandBus.send(Command.AddToQueue(listOf(track)))
                  R.id.track_play_next -> CommandBus.send(Command.PlayNext(track))
                  R.id.track_pin -> CommandBus.send(Command.PinTrack(track))
                  R.id.track_add_to_playlist -> CommandBus.send(Command.AddToPlaylist(listOf(track)))
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

  fun getPositionOf(type: ResultType, position: Int): Int {
    return when (type) {
      ResultType.Artist -> position + 1
      ResultType.Album -> position + artists.size + 2
      ResultType.Track -> artists.size + albums.size + SECTION_COUNT + position
      else -> 0
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
        ResultType.Artist.ordinal -> {
          val position = layoutPosition - 1

          listener?.onArtistClick(view, artists[position])
        }

        ResultType.Album.ordinal -> {
          val position = layoutPosition - artists.size - 2

          listener?.onAlbumClick(view, albums[position])
        }

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
