package com.github.apognu.otter.fragments

import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.Track
import com.github.apognu.otter.utils.mustNormalizeUrl
import com.github.apognu.otter.utils.toDurationString
import kotlinx.android.synthetic.main.fragment_track_info_details.*

class TrackInfoDetailsFragment : DialogFragment() {
  companion object {
    fun new(track: Track): TrackInfoDetailsFragment {
      return TrackInfoDetailsFragment().apply {
        arguments = bundleOf(
          "artistName" to track.artist.name,
          "albumTitle" to track.album.title,
          "trackTitle" to track.title,
          "trackCopyright" to track.copyright,
          "trackLicense" to track.license,
          "trackPosition" to track.position,
          "trackDuration" to track.bestUpload()?.duration?.toLong()?.let { toDurationString(it, showSeconds = true) },
          "trackBitrate" to track.bestUpload()?.bitrate?.let { "${it / 1000} Kbps" },
          "trackInstance" to track.bestUpload()?.listen_url?.let { Uri.parse(mustNormalizeUrl(it)).authority }
        )
      }
    }
  }

  var properties: MutableList<Pair<Int, String?>> = mutableListOf()

  override fun onStart() {
    super.onStart()

    dialog?.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    arguments?.apply {
      properties.add(Pair(R.string.track_info_details_artist, getString("artistName")))
      properties.add(Pair(R.string.track_info_details_album, getString("albumTitle")))
      properties.add(Pair(R.string.track_info_details_track_title, getString("trackTitle")))
      properties.add(Pair(R.string.track_info_details_track_copyright, getString("trackCopyright")))
      properties.add(Pair(R.string.track_info_details_track_license, getString("trackLicense")))
      properties.add(Pair(R.string.track_info_details_track_duration, getString("trackDuration")))
      properties.add(Pair(R.string.track_info_details_track_position, getInt("trackPosition").toString()))
      properties.add(Pair(R.string.track_info_details_track_bitrate, getString("trackBitrate")))
      properties.add(Pair(R.string.track_info_details_track_instance, getString("trackInstance")))
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_track_info_details, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    properties.forEach { (label, value) ->
      val labelTextView = TextView(context).apply {
        text = getString(label)
        setTextAppearance(R.style.AppTheme_TrackDetailsLabel)
      }

      val valueTextView = TextView(context).apply {
        text = value ?: "N/A"
        setTextAppearance(R.style.AppTheme_TrackDetailsValue)
        setPadding(0, 0, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt())
      }

      infos.addView(labelTextView)
      infos.addView(valueTextView)
    }
  }
}