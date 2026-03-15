package com.damn.anotherglass.glass.ee.host.ui.launcher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.example.glass.ui.GlassGestureDetector

class AppLauncherActivity : BaseActivity() {

    data class AppEntry(
        val label: String,
        val icon: Drawable?,
        val launchIntent: Intent
    )

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var snapHelper: PagerSnapHelper
    private lateinit var recyclerView: RecyclerView
    private val apps = mutableListOf<AppEntry>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(FEATURE_VOICE_COMMANDS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_layout)

        apps += queryGlassApps()

        if (apps.isEmpty()) {
            finish()
            return
        }

        recyclerView = findViewById(R.id.menuRecyclerView)
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        snapHelper = PagerSnapHelper()

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = AppAdapter(apps)
        recyclerView.isFocusable = true
        snapHelper.attachToRecyclerView(recyclerView)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val view = snapHelper.findSnapView(layoutManager) ?: return
                currentIndex = layoutManager.getPosition(view)
            }
        })
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        if (featureId != FEATURE_VOICE_COMMANDS) return super.onCreatePanelMenu(featureId, menu)
        menu.clear()
        apps.forEachIndexed { index, app ->
            menu.add(0, index, index, app.label)
        }
        return apps.isNotEmpty()
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean = when (gesture) {
        GlassGestureDetector.Gesture.TAP -> {
            launchCurrent()
            true
        }
        else -> super.onGesture(gesture)
    }

    // Voice command: user said the app name
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val index = item.itemId
        if (index in apps.indices) {
            launchApp(apps[index])
            return true
        }
        return super.onContextItemSelected(item)
    }

    private fun launchCurrent() = launchApp(apps[currentIndex])

    private fun launchApp(app: AppEntry) {
        startActivity(app.launchIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    private fun queryGlassApps(): List<AppEntry> {
        val pm = packageManager
        val baseIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(GLASS_CATEGORY)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(baseIntent, 0)
        return resolveInfos
            .filter { it.activityInfo.packageName != packageName } // exclude self
            .mapNotNull { info ->
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                val launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                    ?: return@mapNotNull null
                AppEntry(label = label, icon = icon, launchIntent = launch)
            }
            .sortedBy { it.label }
    }

    private class AppAdapter(private val items: List<AppEntry>) :
        RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_launcher_item, parent, false)
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = items[position]
            holder.name.text = app.label
            if (app.icon != null) holder.icon.setImageDrawable(app.icon)
            else holder.icon.visibility = View.GONE
        }

        override fun getItemCount() = items.size
    }

    companion object {
        private const val FEATURE_VOICE_COMMANDS = 14
        const val GLASS_CATEGORY = "com.google.android.glass.category.DIRECTORY"
    }
}


