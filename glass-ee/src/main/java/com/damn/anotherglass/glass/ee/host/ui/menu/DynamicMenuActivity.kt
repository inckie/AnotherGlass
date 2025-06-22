package com.damn.anotherglass.glass.ee.host.ui.menu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.ui.BaseActivity
import com.damn.anotherglass.glass.ee.host.ui.menu.MenuActivity.Companion.EXTRA_MENU_ITEM_ID_KEY
import com.example.glass.ui.GlassGestureDetector
import kotlinx.parcelize.Parcelize

// Draft variant of MenuActivity that does not use menu resource
class DynamicMenuActivity : BaseActivity(), GlassGestureDetector.OnGestureListener {

    @Parcelize
    data class DynamicMenuItem(
        val id: Int,
        val text: String,
        @DrawableRes val icon: Int = 0,
        val subtext: String? = null, // not used currently
        val tag: String? = null
    ) : Parcelable

    private val menuItems: MutableList<GlassMenuItem> = ArrayList()
    private var currentMenuItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_layout)

        intent.getParcelableArrayListExtra<DynamicMenuItem>(EXTRA_ITEMS)?.let {
            menuItems += it.map {
                GlassMenuItem(
                    id = it.id,
                    text = it.text,
                    icon = if (it.icon != 0) getDrawable(this, it.icon) else null,
                    tag = it.tag,
                )
            }
        }

        findViewById<RecyclerView>(R.id.menuRecyclerView).apply {
            val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
                this@DynamicMenuActivity,
                LinearLayoutManager.HORIZONTAL, false
            )
            setLayoutManager(layoutManager)
            setAdapter(MenuAdapter(menuItems))
            isFocusable = true
            val snapHelper: SnapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val foundView = snapHelper.findSnapView(layoutManager) ?: return
                    currentMenuItemIndex = layoutManager.getPosition(foundView)
                }
            })
        }

    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean = when (gesture) {
        GlassGestureDetector.Gesture.TAP -> {
            val intent = Intent()
                .putExtra(EXTRA_MENU_ITEM_ID_KEY, menuItems[currentMenuItemIndex].id)
                .putExtra(EXTRA_SELECTED_ITEM_TAG,  menuItems[currentMenuItemIndex].tag)
            setResult(RESULT_OK, intent)
            finish()
            true
        }

        else -> super.onGesture(gesture)
    }

    companion object {
        private const val EXTRA_ITEMS = "EXTRA_ITEMS"
        // selected item id will be returned in MenuActivity.EXTRA_MENU_ITEM_ID_KEY
        const val EXTRA_SELECTED_ITEM_TAG = "tag"

        // Its up to the caller to ensure that items list is not empty
        @JvmStatic
        fun createIntent(activity: Activity, items: ArrayList<DynamicMenuItem>): Intent =
            Intent(activity, DynamicMenuActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_ITEMS, items)
            }
    }
}
