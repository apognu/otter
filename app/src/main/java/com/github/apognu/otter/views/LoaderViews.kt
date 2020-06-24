package com.github.apognu.otter.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.github.apognu.otter.R

object LoadingImageView {
  fun start(context: Context?, image: ImageView): ObjectAnimator? {
    context?.let {
      image.isEnabled = false
      image.setImageDrawable(context.getDrawable(R.drawable.fab_spinner))

      return ObjectAnimator.ofFloat(image, View.ROTATION, 0f, 360f).apply {
        duration = 500
        repeatCount = ObjectAnimator.INFINITE
        start()
      }
    }

    return null
  }

  fun stop(context: Context?, original: Drawable, image: ImageView, animator: ObjectAnimator?) {
    context?.let {
      animator?.cancel()

      image.isEnabled = true
      image.setImageDrawable(original)
      image.rotation = 0.0f
    }
  }
}