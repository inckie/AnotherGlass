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
package com.damn.anotherglass.glass.ee.host.menu

import android.graphics.drawable.Drawable

/**
 * Represents the single menu item object.
 */

/**
 * [GlassMenuItem] object is constructed by usage of this method.
 *
 * @param id is an id of the the current menu item.
 * @param icon is a menu icon [Drawable] object.
 * @param text is a String with the menu option label.
 **/
data class GlassMenuItem(
    val id: Int,
    val icon: Drawable,
    val text: String
)