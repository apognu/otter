package com.github.apognu.otter.utils

import android.os.Build
import android.view.ViewGroup
import android.view.animation.Interpolator
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.transition.TransitionSet
import com.github.apognu.otter.fragments.BrowseFragment
import com.github.apognu.otter.repositories.Repository
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

inline fun <D> Channel<Repository.Response<D>>.await(context: CoroutineContext = Main, crossinline callback: (data: List<D>) -> Unit) {
  GlobalScope.launch(context) {
    this@await.receive().also {
      callback(it.data)
      close()
    }
  }
}

inline fun <D> Channel<Repository.Response<D>>.untilNetwork(context: CoroutineContext = Main, crossinline callback: (data: List<D>) -> Unit) {
  GlobalScope.launch(context) {
    for (data in this@untilNetwork) {
      callback(data.data)

      if (data.origin == Repository.Origin.Network) {
        close()
      }
    }
  }
}

fun TransitionSet.setCommonInterpolator(interpolator: Interpolator): TransitionSet {
  (0 until transitionCount)
    .map { index -> getTransitionAt(index) }
    .forEach { transition -> transition.interpolator = interpolator }

  return this
}

fun Fragment.onViewPager(block: Fragment.() -> Unit) {
  for (f in activity?.supportFragmentManager?.fragments ?: listOf()) {
    if (f is BrowseFragment) {
      f.block()
    }
  }
}

fun Fragment.startTransitions() {
  (view?.parent as? ViewGroup)?.doOnPreDraw {
    startPostponedEnterTransition()
  }
}

fun <T> Int.onApi(block: () -> T) {
  if (Build.VERSION.SDK_INT >= this) {
    block()
  }
}

fun <T, U> Int.onApi(block: () -> T, elseBlock: (() -> U)) {
  if (Build.VERSION.SDK_INT >= this) {
    block()
  } else {
    elseBlock()
  }
}

fun <T> Int.onApiForResult(block: () -> T, elseBlock: (() -> T)): T {
  if (Build.VERSION.SDK_INT >= this) {
    return block()
  } else {
    return elseBlock()
  }
}

fun <T> T.applyOnApi(api: Int, block: T.() -> T): T {
  if (Build.VERSION.SDK_INT >= api) {
    return block()
  } else {
    return this
  }
}