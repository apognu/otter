package com.github.apognu.otter.fragments

import android.os.Bundle
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.RadiosAdapter
import com.github.apognu.otter.repositories.RadiosRepository
import com.github.apognu.otter.utils.*
import kotlinx.android.synthetic.main.fragment_radios.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RadiosFragment : FunkwhaleFragment<Radio, RadiosAdapter>() {
  override val viewRes = R.layout.fragment_radios
  override val recycler: RecyclerView get() = radios

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    adapter = RadiosAdapter(context, lifecycleScope, RadioClickListener())
    repository = RadiosRepository(context)
  }

  inner class RadioClickListener : RadiosAdapter.OnRadioClickListener {
    override fun onClick(holder: RadiosAdapter.ViewHolder, radio: Radio) {
      holder.spin()
      recycler.forEach {
        it.isEnabled = false
        it.isClickable = false
      }

      CommandBus.send(Command.PlayRadio(radio))

      lifecycleScope.launch(Main) {
        EventBus.get().collect { message ->
          when (message) {
            is Event.RadioStarted ->
              if (radios != null) {
                recycler.forEach {
                  it.isEnabled = true
                  it.isClickable = true
                }
              }
          }
        }
      }
    }
  }
}