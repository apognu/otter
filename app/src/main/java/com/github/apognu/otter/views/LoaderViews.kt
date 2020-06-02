package com.github.apognu.otter.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.github.apognu.otter.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

object LoadingFlotingActionButton {
  fun start(button: ExtendedFloatingActionButton): ObjectAnimator {
    button.isEnabled = false
    button.setIconResource(R.drawable.fab_spinner)
    button.shrink()

    return ObjectAnimator.ofFloat(button, View.ROTATION, 0f, 360f).apply {
      duration = 500
      repeatCount = ObjectAnimator.INFINITE
      start()
    }
  }

  fun stop(button: ExtendedFloatingActionButton, animator: ObjectAnimator) {
    animator.cancel()

    button.isEnabled = true
    button.setIconResource(R.drawable.play)
    button.rotation = 0.0f
    button.extend()
  }
}

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