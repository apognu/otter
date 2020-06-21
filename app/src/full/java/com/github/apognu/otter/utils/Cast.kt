package com.github.apognu.otter

import android.content.Context
import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext

object Cast {
  fun init(context: Context) {
    CastContext.getSharedInstance(context)
  }

  fun setupButton(context: Context, menu: Menu?) {
    CastButtonFactory.setUpMediaRouteButton(context, menu, R.id.cast)
  }
}

