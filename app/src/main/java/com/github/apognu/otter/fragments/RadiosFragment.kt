package com.github.apognu.otter.fragments

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.RadiosAdapter
import com.github.apognu.otter.repositories.RadiosRepository
import com.github.apognu.otter.utils.Command
import com.github.apognu.otter.utils.CommandBus
import com.github.apognu.otter.utils.Radio
import kotlinx.android.synthetic.main.fragment_radios.*

class RadiosFragment : FunkwhaleFragment<Radio, RadiosAdapter>() {
  override val viewRes = R.layout.fragment_radios
  override val recycler: RecyclerView get() = radios

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = RadiosAdapter(context, RadioClickListener())
    repository = RadiosRepository(context)
  }

  inner class RadioClickListener : RadiosAdapter.OnRadioClickListener {
    override fun onClick(holder: View?, radio: Radio) {
      CommandBus.send(Command.PlayRadio(radio))
    }
  }
}