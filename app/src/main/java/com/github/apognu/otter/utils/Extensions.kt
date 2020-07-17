package com.github.apognu.otter.utils

import android.os.Build
import androidx.fragment.app.Fragment
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.BrowseFragment
import com.github.apognu.otter.models.api.DownloadInfo
import com.github.apognu.otter.repositories.Repository
import com.github.kittinunf.fuel.core.Request
import com.google.android.exoplayer2.offline.Download
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

inline fun <D> Flow<Repository.Response<D>>.untilNetwork(scope: CoroutineScope, context: CoroutineContext = Main, crossinline callback: (data: List<D>, page: Int, hasMore: Boolean) -> Unit) {
  scope.launch(context) {
    collect { data ->
      callback(data.data, data.page, data.hasMore)
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
  return if (url == null) load(R.drawable.cover)
  else load(url)
}

fun Request.authorize(): Request {
  return this.apply {
    if (!Settings.isAnonymous()) {
      header("Authorization", "Bearer ${Settings.getAccessToken()}")
    }
  }
}

fun Download.getMetadata(): DownloadInfo? = AppContext.json.parse(DownloadInfo.serializer(), String(this.request.data))
