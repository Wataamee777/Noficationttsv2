package com.example.notificationttsv2

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notificationttsv2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: AppAdapter

    private var showSystemApps: Boolean = false
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        showSystemApps = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        loadInstalledApps()
        maybePromptNotificationAccess()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.menu_show_system)?.isChecked = showSystemApps
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_show_system -> {
                val nextChecked = !item.isChecked
                item.isChecked = nextChecked
                showSystemApps = nextChecked
                prefs.edit { putBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps) }
                submitFilteredList()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        // 設定変更の戻りを反映できるよう毎回更新。
        loadInstalledApps()
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter { app, enabled ->
            prefs.edit { putBoolean(keyForPackage(app.packageName), enabled) }
            // ListAdapterの再描画のため、データを更新して再投入。
            allApps = allApps.map {
                if (it.packageName == app.packageName) it.copy(enabled = enabled) else it
            }
            submitFilteredList()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadInstalledApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .map { appInfo ->
                val packageName = appInfo.packageName
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val enabled = prefs.getBoolean(keyForPackage(packageName), true)
                AppInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    isSystemApp = isSystem,
                    enabled = enabled
                )
            }
            .sortedBy { it.appName.lowercase() }

        allApps = apps
        submitFilteredList()
    }

    private fun submitFilteredList() {
        val filtered = if (showSystemApps) {
            allApps
        } else {
            allApps.filterNot { it.isSystemApp }
        }
        adapter.submitList(filtered)
    }

    private fun maybePromptNotificationAccess() {
        if (isNotificationListenerEnabled()) return

        AlertDialog.Builder(this)
            .setMessage(R.string.notification_access_required)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val myComponent = ComponentName(this, NotificationService::class.java).flattenToString()
        return enabledListeners.split(":").any { it == myComponent }
    }

    private fun keyForPackage(packageName: String): String = "enabled_$packageName"

    companion object {
        const val PREFS_NAME = "notification_tts_prefs"
        const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    }
}
