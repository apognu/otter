package com.github.apognu.otter.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.DownloadsAdapter
import com.github.apognu.otter.utils.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_downloads.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DownloadsActivity : AppCompatActivity() {
  lateinit var adapter: DownloadsAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_downloads)

    adapter = DownloadsAdapter(this, RefreshListener()).also {
      downloads.layoutManager = LinearLayoutManager(this)
      downloads.adapter = it
    }

    GlobalScope.launch(Main) {
      while (true) {
        refresh()
        delay(1000)
      }
    }
  }

  private fun refresh() {
    GlobalScope.launch(Main) {
      RequestBus.send(Request.GetDownloads).wait<Response.Downloads>()?.let { response ->
        adapter.downloads.clear()

        while (response.cursor.moveToNext()) {
          val download = response.cursor.download

          Gson().fromJson(String(download.request.data), DownloadInfo::class.java)?.let { info ->
            adapter.downloads.add(info.apply {
              this.download = download
            })
          }
        }

        adapter.notifyDataSetChanged()
      }
    }
  }

  inner class RefreshListener : DownloadsAdapter.OnRefreshListener {
    override fun refresh() {
      this@DownloadsActivity.refresh()
    }
  }
}