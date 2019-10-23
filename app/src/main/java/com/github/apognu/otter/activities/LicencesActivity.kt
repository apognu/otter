package com.github.apognu.otter.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import kotlinx.android.synthetic.main.activity_licences.*
import kotlinx.android.synthetic.main.row_licence.view.*

class LicencesActivity : AppCompatActivity() {
  data class Licence(val name: String, val licence: String, val url: String)

  interface OnLicenceClickListener {
    fun onClick(url: String)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_licences)

    LicencesAdapter(OnLicenceClick()).also {
      licences.layoutManager = LinearLayoutManager(this)
      licences.adapter = it
    }
  }

  private inner class LicencesAdapter(val listener: OnLicenceClickListener) : RecyclerView.Adapter<LicencesAdapter.ViewHolder>() {
    val licences = listOf(
      Licence(
        "ExoPlayer",
        "Apache License 2.0",
        "https://github.com/google/ExoPlayer/blob/release-v2/LICENSE"
      ),
      Licence(
        "ExoPlayer-Extensions",
        "Apache License 2.0",
        "https://github.com/PaulWoitaschek/ExoPlayer-Extensions/blob/master/LICENSE"
      ),
      Licence(
        "Fuel",
        "MIT License",
        "https://github.com/kittinunf/fuel/blob/master/LICENSE.md"
      ),
      Licence(
        "Gson",
        "Apache License 2.0",
        "https://github.com/google/gson/blob/master/LICENSE"
      ),
      Licence(
        "Picasso",
        "Apache License 2.0",
        "https://github.com/square/picasso/blob/master/LICENSE.txt"
      ),
      Licence(
        "Picasso Transformations",
        "Apache License 2.0",
        "https://github.com/wasabeef/picasso-transformations/blob/master/LICENSE"
      ),
      Licence(
        "PowerPreference",
        "Apache License 2.0",
        "https://github.com/AliAsadi/PowerPreference/blob/master/LICENSE"
      )
    )

    override fun getItemCount() = licences.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val view = LayoutInflater.from(this@LicencesActivity).inflate(R.layout.row_licence, parent, false)

      return ViewHolder(view).also {
        view.setOnClickListener(it)
      }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val item = licences[position]

      holder.name.text = item.name
      holder.licence.text = item.licence
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
      val name = view.name
      val licence = view.licence

      override fun onClick(view: View?) {
        listener.onClick(licences[layoutPosition].url)
      }
    }
  }

  inner class OnLicenceClick : OnLicenceClickListener {
    override fun onClick(url: String) {
      Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        startActivity(this)
      }
    }
  }
}