package com.github.apognu.otter.utils

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.github.apognu.otter.R
import com.github.apognu.otter.fragments.BrowseFragment
import com.github.apognu.otter.repositories.Repository
import com.github.kittinunf.fuel.core.Request
import com.google.android.exoplayer2.offline.Download
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import net.openid.appauth.ClientSecretPost
import kotlin.coroutines.CoroutineContext

inline fun <D> Flow<Repository.Response<D>>.untilNetwork(scope: CoroutineScope, context: CoroutineContext = Main, crossinline callback: (data: List<D>, isCache: Boolean, page: Int, hasMore: Boolean) -> Unit) {
  scope.launch(context) {
    collect { data ->
      callback(data.data, data.origin == Repository.Origin.Cache, data.page, data.hasMore)
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

fun Request.authorize(context: Context): Request {
  return runBlocking {
    this@authorize.apply {
      if (!Settings.isAnonymous()) {
        OAuth.state().let { state ->
          val old = state.accessToken
          val auth = ClientSecretPost(OAuth.state().clientSecret)
          val done = CompletableDeferred<Boolean>()

          state.performActionWithFreshTokens(OAuth.service(context), auth) { token, _, _ ->
            if (token != old && token != null) {
              state.save()
            }

            header("Authorization", "Bearer ${OAuth.state().accessToken}")
            done.complete(true)
          }

          done.await()

          return@runBlocking this
        }
      }
    }
  }
}

fun Download.getMetadata(): DownloadInfo? = Gson().fromJson(String(this.request.data), DownloadInfo::class.java)
