/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.damn.anotherglass.glass.ee.host.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.damn.anotherglass.glass.ee.host.databinding.MenuItemBinding
import com.damn.anotherglass.glass.ee.host.ui.menu.MenuAdapter.MenuViewHolder

/**
 * Adapter for the menu horizontal recycler view.
 */
class MenuAdapter internal constructor(private val menuItems: List<GlassMenuItem>) :
    RecyclerView.Adapter<MenuViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder =
        MenuViewHolder(MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(menuViewHolder: MenuViewHolder, position: Int) =
        menuViewHolder.bind(menuItems[position])

    override fun getItemCount(): Int = menuItems.size

    class MenuViewHolder(private val binding: MenuItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(glassMenuItem: GlassMenuItem?) {
            binding.setItem(glassMenuItem)
            binding.executePendingBindings()
        }
    }
}
