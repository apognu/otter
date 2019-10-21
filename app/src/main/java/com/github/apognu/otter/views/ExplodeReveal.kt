package com.github.apognu.otter.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionValues
import androidx.transition.Visibility

class ExplodeReveal : Visibility() {
  private val SCREEN_BOUNDS = "screenBounds"

  private val locations = IntArray(2)

  override fun captureStartValues(transitionValues: TransitionValues) {
    super.captureStartValues(transitionValues)

    capture(transitionValues)
  }

  override fun captureEndValues(transitionValues: TransitionValues) {
    super.captureEndValues(transitionValues)

    capture(transitionValues)
  }

  override fun onAppear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
    if (endValues == null) return null

    val bounds = endValues.values[SCREEN_BOUNDS] as Rect

    val endY = view.translationY
    val distance = calculateDistance(sceneRoot, bounds)
    val startY = endY + distance

    return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
  }

  override fun onDisappear(sceneRoot: ViewGroup, view: View, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
    if (startValues == null) return null

    val bounds = startValues.values[SCREEN_BOUNDS] as Rect

    val startY = view.translationY
    val distance = calculateDistance(sceneRoot, bounds)
    val endY = startY + distance

    return ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY)
  }

  private fun capture(transitionValues: TransitionValues) {
    transitionValues.view.also {
      it.getLocationOnScreen(locations)

      val left = locations[0]
      val top = locations[1]
      val right = left + it.width
      val bottom = top + it.height

      transitionValues.values[SCREEN_BOUNDS] = Rect(left, top, right, bottom)
    }
  }

  private fun calculateDistance(sceneRoot: View, viewBounds: Rect): Int {
    sceneRoot.getLocationOnScreen(locations)

    val sceneRootY = locations[1]

    return when (epicenter) {
      is Rect -> return when {
        viewBounds.top <= (epicenter as Rect).top -> sceneRootY - (epicenter as Rect).top
        else -> sceneRootY + sceneRoot.height - (epicenter as Rect).bottom
      }

      else -> -sceneRoot.height
    }
  }
}