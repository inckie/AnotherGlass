package com.damn.anotherglass.glass.ee.host.ui

import com.damn.anotherglass.glass.ee.host.ui.cards.BaseFragment

interface ITimeline {
    fun addFragment(fragment: BaseFragment, index: Int = 0, scrollTo: Boolean = true)
    fun removeFragment(tag: String)
    fun <T: BaseFragment> removeByType(cls: Class<T>)
    fun <T: BaseFragment> indexOfFirst(java: Class<T>): Int
    fun setCurrent(index: Int, smoothScroll: Boolean)
}
