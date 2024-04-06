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
package com.damn.anotherglass.glass.ee.host.fragments

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.menu.MenuActivity

/**
 * Base class for each fragment. Provides functionality to start new activity with a menu.
 */
abstract class BaseFragment : Fragment(), OnSingleTapUpListener {
    override fun onSingleTapUp() {
        if (arguments != null) {
            val args = requireArguments()
            val menu = args.getInt(MENU_KEY, MENU_DEFAULT_VALUE)
            if (menu != MENU_DEFAULT_VALUE) {
                val intent = Intent(activity, MenuActivity::class.java)
                intent.putExtra(MENU_KEY, menu)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    /**
     * Code for a response to selected menu item should be placed inside of this method.
     *
     * @param requestCode is a code which should match the [BaseFragment.REQUEST_CODE]
     * @param resultCode is a code set by the [com.damn.anotherglass.glass.ee.host.menu.MenuActivity]
     * @param data is a id passed by the [MenuActivity.EXTRA_MENU_ITEM_ID_KEY] key. Refers to
     * the selected menu option
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val id = data.getIntExtra(
                MenuActivity.EXTRA_MENU_ITEM_ID_KEY,
                MenuActivity.EXTRA_MENU_ITEM_DEFAULT_VALUE
            )
            var selectedOption = ""
            when (id) {
                R.id.add -> selectedOption = getString(R.string.add)
                R.id.save -> selectedOption = getString(R.string.save)
                R.id.delete -> selectedOption = getString(R.string.delete)
            }
            Toast.makeText(activity, "$selectedOption option selected.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /**
         * Key for obtaining menu value from fragment arguments.
         */
        /*protected*/ const val MENU_KEY = "menu_key" // protected disabled due to Kotlin bug

        /**
         * Default value for menu obtained from fragment arguments.
         */
        protected const val MENU_DEFAULT_VALUE = 0

        /**
         * Request code for starting activity for result. This value doesn't have any special meaning.
         */
        protected const val REQUEST_CODE = 205
    }
}
