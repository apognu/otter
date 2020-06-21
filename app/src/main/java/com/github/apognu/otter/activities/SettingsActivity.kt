package com.github.apognu.otter.activities

import android.content.*
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.github.apognu.otter.BuildConfig
import com.github.apognu.otter.Otter
import com.github.apognu.otter.R
import com.github.apognu.otter.utils.*
import com.preference.PowerPreference

class SettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_settings)

    supportFragmentManager
      .beginTransaction()
      .replace(
        R.id.container,
        SettingsFragment()
      )
      .commit()
  }

  fun getThemeResId(): Int = R.style.AppTheme
}

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
  override fun onResume() {
    super.onResume()

    preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    setPreferencesFromResource(R.xml.settings, rootKey)

    updateValues()
  }

  override fun onPreferenceTreeClick(preference: Preference?): Boolean {
    when (preference?.key) {
      "oss_licences" -> startActivity(Intent(activity, LicencesActivity::class.java))

      "experiments" -> {
        context?.let { context ->
          AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.settings_experiments_restart_title))
            .setMessage(context.getString(R.string.settings_experiments_restart_content))
            .setPositiveButton(android.R.string.yes) { _, _ -> }
            .show()
        }
      }

      "crash" -> {
        activity?.let { activity ->
          (activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.also { clip ->
            Cache.get(activity, "crashdump")?.readLines()?.joinToString("\n").also {
              clip.setPrimaryClip(ClipData.newPlainText("Otter logs", it))

              Toast.makeText(activity, activity.getString(R.string.settings_crash_report_copied), Toast.LENGTH_SHORT).show()
            }
          }
        }
      }

      "logout" -> {
        context?.let { context ->
          AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.logout_title))
            .setMessage(context.getString(R.string.logout_content))
            .setPositiveButton(android.R.string.yes) { _, _ ->
              CommandBus.send(Command.ClearQueue)

              Otter.get().deleteAllData()

              activity?.setResult(MainActivity.ResultCode.LOGOUT.code)
              activity?.finish()
            }
            .setNegativeButton(android.R.string.no, null)
            .show()
        }
      }
    }

    updateValues()

    return super.onPreferenceTreeClick(preference)
  }

  override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
    updateValues()
  }

  private fun updateValues() {
    (activity as? AppCompatActivity)?.let { activity ->
      preferenceManager.findPreference<ListPreference>("media_quality")?.let {
        it.summary = when (it.value) {
          "quality" -> activity.getString(R.string.settings_media_quality_summary_quality)
          "size" -> activity.getString(R.string.settings_media_quality_summary_size)
          else -> activity.getString(R.string.settings_media_quality_summary_size)
        }
      }

      preferenceManager.findPreference<ListPreference>("night_mode")?.let {
        when (it.value) {
          "on" -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES

            it.summary = getString(R.string.settings_night_mode_on_summary)
          }

          "off" -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO

            it.summary = getString(R.string.settings_night_mode_off_summary)
          }

          else -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            activity.delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

            it.summary = getString(R.string.settings_night_mode_system_summary)
          }
        }
      }

      preferenceManager.findPreference<SeekBarPreference>("media_cache_size")?.let {
        it.summary = getString(R.string.settings_media_cache_size_summary, it.value)
      }

      preferenceManager.findPreference<Preference>("version")?.let {
        it.summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
      }
    }
  }
}