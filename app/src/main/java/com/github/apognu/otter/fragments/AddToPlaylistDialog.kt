package com.github.apognu.otter.fragments

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.PlaylistsAdapter
import com.github.apognu.otter.repositories.ManagementPlaylistsRepository
import com.github.apognu.otter.utils.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.dialog_add_to_playlist.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

object AddToPlaylistDialog {
  fun show(activity: Activity, lifecycleScope: CoroutineScope, track: Track) {
    val dialog = AlertDialog.Builder(activity).run {
      setTitle("Add track to playlist")
      setView(activity.layoutInflater.inflate(R.layout.dialog_add_to_playlist, null))

      create()
    }

    dialog.show()

    val repository = ManagementPlaylistsRepository(activity)

    dialog.name.editText?.addTextChangedListener {
      dialog.create.isEnabled = !(dialog.name.editText?.text?.trim()?.isBlank() ?: true)
    }

    dialog.create.setOnClickListener {
      val name = dialog.name.editText?.text?.toString()?.trim() ?: ""

      if (name.isEmpty()) return@setOnClickListener

      lifecycleScope.launch(IO) {
        repository.new(name)?.let { id ->
          repository.add(id, track)
          dialog.dismiss()
        }
      }
    }

    val adapter = PlaylistsAdapter(activity, object : PlaylistsAdapter.OnPlaylistClickListener {
      override fun onClick(holder: View?, playlist: Playlist) {
        repository.add(playlist.id, track)
        dialog.dismiss()
      }
    })

    dialog.playlists.layoutManager = LinearLayoutManager(activity)
    dialog.playlists.adapter = adapter

    repository.apply {
      var first = true

      fetch().untilNetwork(lifecycleScope) { data, isCache, _, hasMore ->
        if (isCache) {
          adapter.data = data.toMutableList()
          adapter.notifyDataSetChanged()

          return@untilNetwork
        }

        if (first) {
          adapter.data.clear()
          first = false
        }

        adapter.data.addAll(data)

        lifecycleScope.launch(IO) {
          try {
            Cache.set(
              context,
              cacheId,
              Gson().toJson(cache(adapter.data)).toByteArray()
            )
          } catch (e: ConcurrentModificationException) {
          }
        }

        if (!hasMore) {
          adapter.notifyDataSetChanged()
          first = false
        }
      }
    }
  }
}