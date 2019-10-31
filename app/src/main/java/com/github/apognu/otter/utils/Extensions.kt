package com.github.apognu.otter.utils

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.BrowseFragment
import com.github.apognu.otter.repositories.Repository
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

fun Context.getColor(colorRes: Int): Int {
  return ContextCompat.getColor(this, colorRes)
}

inline fun <D> Flow<Repository.Response<D>>.untilNetwork(context: CoroutineContext = Main, crossinline callback: (data: List<D>, isCache: Boolean, hasMore: Boolean) -> Unit) {
  GlobalScope.launch(context) {
    collect { data ->
      callback(data.data, data.origin == Repository.Origin.Cache, data.hasMore)
    }
  }
}

fun Fragment.onViewPager(block: Fragment.() -> Unit) {
  for (f in activity?.supportFragmentManager?.fragments ?: listOf()) {
    if (f is BrowseFragment) {
      f.block()
    }
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
  return if (Build.VERSION.SDK_INT >= this) {
    block()
  } else {
    elseBlock()
  }
}

fun <T> T.applyOnApi(api: Int, block: T.() -> T): T {
  return if (Build.VERSION.SDK_INT >= api) {
    block()
  } else {
    this
  }
}

fun Picasso.maybeLoad(url: String?): RequestCreator {
  if (url == null) return load(R.drawable.cover)
  else return load(url)
}