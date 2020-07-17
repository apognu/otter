package com.github.apognu.otter.fragments

import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.github.apognu.otter.R
import com.github.apognu.otter.adapters.RadiosAdapter
import com.github.apognu.otter.models.api.FunkwhaleRadio
import com.github.apognu.otter.models.dao.RadioEntity
import com.github.apognu.otter.repositories.RadiosRepository
import com.github.apognu.otter.utils.*
import com.github.apognu.otter.viewmodels.RadiosViewModel
import kotlinx.android.synthetic.main.fragment_radios.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class RadiosFragment : LiveOtterFragment<FunkwhaleRadio, RadioEntity, RadiosAdapter>() {
  override val repository by inject<RadiosRepository>()
  override val adapter by inject<RadiosAdapter> { parametersOf(context, lifecycleScope, RadioClickListener()) }
  override val viewModel by inject<RadiosViewModel>()
  override val liveData by lazy { viewModel.radios }

  override val viewRes = R.layout.fragment_radios
  override val recycler: RecyclerView get() = radios
  override val alwaysRefresh = false

  inner class RadioClickListener : RadiosAdapter.OnRadioClickListener {
    override fun onClick(holder: RadiosAdapter.ViewHolder, radio: RadioEntity) {
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