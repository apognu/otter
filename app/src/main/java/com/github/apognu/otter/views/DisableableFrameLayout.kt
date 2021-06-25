package com.github.apognu.otter.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class DisableableFrameLayout : FrameLayout {
  var callback: ((MotionEvent?) -> Boolean)? = null

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, style: Int) : super(context, attrs, style)

  override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
    callback?.let {
      return !it(event)
    }

    return false
  }

  fun setShouldRegisterTouch(callback: (event: MotionEvent?) -> Boolean) {
    this.callback = callback
  }
}
