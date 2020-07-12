package com.github.apognu.otter.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import com.github.apognu.otter.R
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.partial_now_playing.view.*
import kotlin.math.abs
import kotlin.math.min

class NowPlayingView : MaterialCardView {
  val activity: Context
  var gestureDetector: GestureDetector? = null
  var gestureDetectorCallback: OnGestureDetection? = null

  constructor(context: Context) : super(context) {
    activity = context
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    activity = context
  }

  constructor(context: Context, attrs: AttributeSet?, style: Int) : super(context, attrs, style) {
    activity = context
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    now_playing_root.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.UNSPECIFIED))
  }

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)

    if (visibility == View.VISIBLE && gestureDetector == null) {
      viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
          gestureDetectorCallback = OnGestureDetection()
          gestureDetector = GestureDetector(context, gestureDetectorCallback)

          setOnTouchListener { _, motionEvent ->
            val ret = gestureDetector?.onTouchEvent(motionEvent) ?: false

            if (motionEvent.actionMasked == MotionEvent.ACTION_UP) {
              if (gestureDetectorCallback?.isScrolling == true) {
                gestureDetectorCallback?.onUp()
              }
            }

            ret
          }

          viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
      })
    }
  }

  fun isOpened(): Boolean = gestureDetectorCallback?.isOpened() ?: false

  fun close() {
    gestureDetectorCallback?.close()
  }

  inner class OnGestureDetection : GestureDetector.SimpleOnGestureListener() {
    private var maxHeight = 0
    private var minHeight = 0
    private var maxMargin = 0

    private var initialTouchY = 0f
    private var lastTouchY = 0f

    var isScrolling = false
    private var flingAnimator: ValueAnimator? = null

    init {
      (layoutParams as? MarginLayoutParams)?.let {
        maxMargin = it.marginStart
      }

      minHeight = TypedValue().let {
        activity.theme.resolveAttribute(R.attr.actionBarSize, it, true)

        TypedValue.complexToDimensionPixelSize(it.data, resources.displayMetrics)
      }

      maxHeight = now_playing_details.measuredHeight + (2 * maxMargin)
    }

    override fun onDown(e: MotionEvent): Boolean {
      initialTouchY = e.rawY
      lastTouchY = e.rawY

      return true
    }

    fun onUp(): Boolean {
      isScrolling = false

      layoutParams.let {
        val offsetToMax = maxHeight - height
        val offsetToMin = height - minHeight

        flingAnimator =
          if (offsetToMin < offsetToMax) ValueAnimator.ofInt(it.height, minHeight)
          else ValueAnimator.ofInt(it.height, maxHeight)

        animateFling(500)

        return true
      }
    }

    override fun onFling(firstMotionEvent: MotionEvent?, secondMotionEvent: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
      isScrolling = false

      layoutParams.let {
        val diff =
          if (velocityY < 0) maxHeight - it.height
          else it.height - minHeight

        flingAnimator =
          if (velocityY < 0) ValueAnimator.ofInt(it.height, maxHeight)
          else ValueAnimator.ofInt(it.height, minHeight)

        animateFling(min(abs((diff.toFloat() / velocityY * 1000).toLong()), 600))
      }

      return true
    }

    override fun onScroll(firstMotionEvent: MotionEvent, secondMotionEvent: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
      isScrolling = true

      layoutParams.let {
        val newHeight = it.height + lastTouchY - secondMotionEvent.rawY
        val progress = (newHeight - minHeight) / (maxHeight - minHeight)
        val newMargin = maxMargin - (maxMargin * progress)

        (layoutParams as? MarginLayoutParams)?.let {
          it.marginStart = newMargin.toInt()
          it.marginEnd = newMargin.toInt()
          it.bottomMargin = newMargin.toInt()
        }

        layoutParams = layoutParams.apply {
          when {
            newHeight <= minHeight -> {
              height = minHeight
              return true
            }
            newHeight >= maxHeight -> {
              height = maxHeight
              return true
            }
            else -> height = newHeight.toInt()
          }
        }

        summary.alpha = 1f - progress

        summary.layoutParams = summary.layoutParams.apply {
          height = (minHeight * (1f - progress)).toInt()
        }
      }

      lastTouchY = secondMotionEvent.rawY

      return true
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
      layoutParams.let {
        if (height != minHeight) return true

        flingAnimator = ValueAnimator.ofInt(it.height, maxHeight)

        animateFling(300)
      }

      return true
    }

    fun isOpened(): Boolean = layoutParams.height == maxHeight

    fun close(): Boolean {
      layoutParams.let {
        if (it.height == minHeight) return true

        flingAnimator = ValueAnimator.ofInt(it.height, minHeight)

        animateFling(300)
      }

      return true
    }

    private fun animateFling(dur: Long) {
      flingAnimator?.apply {
        duration = dur
        interpolator = DecelerateInterpolator()

        addUpdateListener { valueAnimator ->
          layoutParams = layoutParams.apply {
            val newHeight = valueAnimator.animatedValue as Int
            val progress = (newHeight.toFloat() - minHeight) / (maxHeight - minHeight)
            val newMargin = maxMargin - (maxMargin * progress)

            (layoutParams as? MarginLayoutParams)?.let {
              it.marginStart = newMargin.toInt()
              it.marginEnd = newMargin.toInt()
              it.bottomMargin = newMargin.toInt()
            }

            height = newHeight

            summary.alpha = 1f - progress

            summary.layoutParams = summary.layoutParams.apply {
              height = (minHeight * (1f - progress)).toInt()
            }
          }
        }

        start()
      }
    }
  }
}
