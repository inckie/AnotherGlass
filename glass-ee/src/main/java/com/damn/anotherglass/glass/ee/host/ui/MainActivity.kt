/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.damn.anotherglass.glass.ee.host.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.ColumnLayoutFragment
import com.damn.anotherglass.glass.ee.host.ui.cards.TextLayoutFragment
import com.example.glass.ui.GlassGestureDetector
import com.google.android.material.tabs.TabLayout

/**
 * Main activity of the application. It provides viewPager to move between fragments.
 */
class MainActivity : BaseActivity() {
    private val fragments: MutableList<BaseFragment> = ArrayList()
    private lateinit var viewPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_pager_layout)
        viewPager = findViewById(R.id.viewPager)

        fragments.add(
            TextLayoutFragment.newInstance(
                getString(R.string.text_sample), getString(R.string.footnote_sample),
                getString(R.string.timestamp_sample), null
            )
        )
        fragments.add(
            TextLayoutFragment.newInstance(
                getString(R.string.different_options), getString(R.string.empty_string),
                getString(R.string.empty_string), R.menu.main_menu
            )
        )
        fragments.add(
            ColumnLayoutFragment
                .newInstance(
                    R.drawable.ic_style, getString(R.string.columns_sample),
                    getString(R.string.footnote_sample), getString(R.string.timestamp_sample)
                )
        )
        fragments.add(
            TextLayoutFragment.newInstance(
                getString(R.string.like_this_sample), getString(R.string.empty_string),
                getString(R.string.empty_string), null
            )
        )
        viewPager.setAdapter(ScreenSlidePagerAdapter(supportFragmentManager))

        val tabLayout = findViewById<TabLayout>(R.id.page_indicator)
        tabLayout.setupWithViewPager(viewPager, true)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean =
        when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                fragments[viewPager.currentItem].onSingleTapUp()
                true
            }

            else -> super.onGesture(gesture)
        }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment = fragments[position]

        override fun getCount(): Int = fragments.size
    }
}
