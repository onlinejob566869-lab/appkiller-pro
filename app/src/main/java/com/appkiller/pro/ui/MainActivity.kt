package com.appkiller.pro.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.appkiller.pro.AppKillerApp
import com.appkiller.pro.R
import com.appkiller.pro.adapter.AppListAdapter
import com.appkiller.pro.data.AppRepository
import com.appkiller.pro.model.AppInfo
import com.appkiller.pro.service.AppKillerAccessibilityService
import com.appkiller.pro.util.KillCounter
import com.appkiller.pro.util.WhitelistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: AppListAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: EditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: TextView
    private lateinit var accessibilityWarning: LinearLayout
    private lateinit var killAllBtn: Button
    private lateinit var killSelectedBtn: Button
    private lateinit var memoryProgress: ProgressBar
    private lateinit var usedMemoryText: TextView
    private lateinit var freeMemoryText: TextView
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupListeners()
        handleDeepLink(intent)
        loadApps()
        updateMemoryInfo()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.appRecyclerView)
        searchBar = findViewById(R.id.searchBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        emptyState = findViewById(R.id.emptyState)
        accessibilityWarning = findViewById(R.id.accessibilityWarning)
        killAllBtn = findViewById(R.id.killAllBtn)
        killSelectedBtn = findViewById(R.id.killSelectedBtn)
        memoryProgress = findViewById(R.id.memoryProgress)
        usedMemoryText = findViewById(R.id.usedMemoryText)
        freeMemoryText = findViewById(R.id.freeMemoryText)

        accessibilityWarning.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun setupRecyclerView() {
        adapter = AppListAdapter(
            onKillClick = { app -> killSingleApp(app) },
            onCheckChange = { _, _ -> },
            onWhitelistToggle = { app -> toggleWhitelist(app) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s.toString())
            }
        })

        swipeRefresh.setOnRefreshListener {
            loadApps()
            updateMemoryInfo()
        }

        killAllBtn.setOnClickListener { killAllApps() }
        killSelectedBtn.setOnClickListener { killSelectedApps() }
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "appkiller" && data.host == "action" && data.path?.startsWith("/stop") == true) {
            killAllApps()
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            swipeRefresh.isRefreshing = true
            val showSystem = WhitelistManager.showSystemApps(this@MainActivity)
            val apps = withContext(Dispatchers.IO) {
                AppRepository.getInstalledApps(showSystem)
            }
            allApps = apps
            adapter.updateApps(apps)
            swipeRefresh.isRefreshing = false
            emptyState.visibility = if (apps.isEmpty()) View.VISIBLE else View.GONE
            checkAccessibilityStatus()
        }
    }

    private fun filterApps(query: String) {
        if (query.isBlank()) {
            adapter.updateApps(allApps)
        } else {
            val filtered = allApps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
            adapter.updateApps(filtered)
        }
    }

    private fun killSingleApp(app: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_kill))
            .setMessage(getString(R.string.confirm_kill_msg, app.appName))
            .setPositiveButton(getString(R.string.confirm_kill)) { _, _ ->
                AppKillerAccessibilityService.Companion.killPackage(app.packageName)
                KillCounter.incrementKills()
                Toast.makeText(this, "${app.appName} force stopped", Toast.LENGTH_SHORT).show()
                loadApps()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun killAllApps() {
        if (!AppKillerApp.isAccessibilityEnabled(this)) {
            showAccessibilityDialog()
            return
        }

        val killable = allApps.filter { it.isRunning && !it.isWhitelisted }
        if (killable.isEmpty()) {
            Toast.makeText(this, "No running apps to kill", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.kill_all))
            .setMessage("Force stop ${killable.size} running apps?")
            .setPositiveButton(getString(R.string.confirm_kill)) { _, _ ->
                val packages = killable.map { it.packageName }
                val killed = AppKillerAccessibilityService.Companion.killPackages(packages)
                KillCounter.incrementKills(killed)
                Toast.makeText(this, getString(R.string.killed_apps, killed), Toast.LENGTH_SHORT).show()
                loadApps()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun killSelectedApps() {
        val selected = adapter.getSelectedApps()
        if (selected.isEmpty()) {
            Toast.makeText(this, "No apps selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (!AppKillerApp.isAccessibilityEnabled(this)) {
            showAccessibilityDialog()
            return
        }
        val packages = selected.map { it.packageName }
        val killed = AppKillerAccessibilityService.Companion.killPackages(packages)
        KillCounter.incrementKills(killed)
        Toast.makeText(this, getString(R.string.killed_apps, killed), Toast.LENGTH_SHORT).show()
        loadApps()
    }

    private fun toggleWhitelist(app: AppInfo) {
        if (WhitelistManager.isWhitelisted(this, app.packageName)) {
            WhitelistManager.removeFromWhitelist(this, app.packageName)
            Toast.makeText(this, "${app.appName} removed from whitelist", Toast.LENGTH_SHORT).show()
        } else {
            WhitelistManager.addToWhitelist(this, app.packageName)
            Toast.makeText(this, "${app.appName} added to whitelist", Toast.LENGTH_SHORT).show()
        }
        loadApps()
    }

    private fun checkAccessibilityStatus() {
        if (AppKillerApp.isAccessibilityEnabled(this)) {
            accessibilityWarning.visibility = View.GONE
        } else {
            accessibilityWarning.visibility = View.VISIBLE
        }
    }

    private fun updateMemoryInfo() {
        lifecycleScope.launch {
            val memInfo = withContext(Dispatchers.IO) { AppRepository.getMemoryInfo() }
            val used = memInfo.totalMem - memInfo.availMem
            val percent = ((used.toDouble() / memInfo.totalMem) * 100).toInt()
            memoryProgress.progress = percent
            usedMemoryText.text = "Used: ${AppInfo.formatSize(used)}"
            freeMemoryText.text = "Free: ${AppInfo.formatSize(memInfo.availMem)}"
        }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.enable_accessibility))
            .setMessage("AppKiller needs the Accessibility Service to force-stop apps. Please enable it in Settings.")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()
        loadApps()
        updateMemoryInfo()
    }
}
