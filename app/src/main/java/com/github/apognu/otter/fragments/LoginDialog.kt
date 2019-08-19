package com.github.apognu.otter.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.github.apognu.otter.R

class LoginDialog : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(context).apply {
      setTitle(getString(R.string.login_logging_in))
      setView(R.layout.dialog_login)
    }.create()
  }

  override fun onResume() {
    super.onResume()

    dialog?.setCanceledOnTouchOutside(false)
    dialog?.setCancelable(false)
  }
}