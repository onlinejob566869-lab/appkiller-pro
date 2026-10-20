package com.appkiller.pro.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.appkiller.pro.R
import com.appkiller.pro.service.AutoKillService
import com.appkiller.pro.util.KillCounter
import com.appkiller.pro.util.WhitelistManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Auto-Kill switch
            findPreference<SwitchPreferenceCompat>("auto_kill_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                val context = requireContext()
                if (enabled) {
                    AutoKillService.startService(context)
                } else {
                    AutoKillService.stopService(context)
                }
                true
            }

            // Show system apps switch
            findPreference<SwitchPreferenceCompat>("show_system_apps")?.setOnPreferenceChangeListener { _, _ ->
                true
            }

            // Sort preference
            findPreference<Preference>("sort_by")?.setOnPreferenceChangeListener { _, _ ->
                true
            }

            // Reset kill count
            findPreference<Preference>("kill_count")?.setOnPreferenceClickListener {
                KillCounter.reset()
                findPreference<Preference>("kill_count")?.summary = "Total Kills: ${KillCounter.getTotalKills()}"
                true
            }

            // Update kill count summary
            findPreference<Preference>("kill_count")?.summary = "Total Kills: ${KillCounter.getTotalKills()}"
        }
    }
}
