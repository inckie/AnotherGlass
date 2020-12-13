package com.damn.anotherglass.logging

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.ViewPager
import com.applicaster.xray.ui.adapters.ViewsPagerAdapter
import com.damn.anotherglass.R

class LogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        val pager: ViewPager = findViewById(R.id.pager)
        pager.adapter = ViewsPagerAdapter(pager)
    }
}