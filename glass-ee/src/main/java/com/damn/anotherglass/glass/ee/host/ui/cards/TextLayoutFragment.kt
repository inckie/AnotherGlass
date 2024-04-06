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
package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.damn.anotherglass.glass.ee.host.R

/**
 * CardBuilder.Layout.TEXT_FIXED but without images support.
 * https://developers.google.com/glass/develop/gdk/card-design#text_and_text_fixed
 */
class TextLayoutFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = requireArguments()

        val view = inflater.inflate(R.layout.main_layout, container, false)

        val textView = TextView(context)
        textView.text = args.getString(TEXT_KEY, getString(R.string.empty_string))
        textView.textSize = BODY_TEXT_SIZE.toFloat()
        textView.setTypeface(Typeface.create(getString(R.string.thin_font), Typeface.NORMAL))
        val bodyLayout = view.findViewById<FrameLayout>(R.id.body_layout)
        bodyLayout.addView(textView)

        val footer = view.findViewById<TextView>(R.id.footer)
        footer.text = args.getString(FOOTER_KEY, getString(R.string.empty_string))
        val timestamp = view.findViewById<TextView>(R.id.timestamp)
        timestamp.text = args.getString(TIMESTAMP_KEY, getString(R.string.empty_string))
        return view
    }

    companion object {
        private const val TEXT_KEY = "text_key"
        private const val FOOTER_KEY = "footer_key"
        private const val TIMESTAMP_KEY = "timestamp_key"
        private const val BODY_TEXT_SIZE = 40

        /**
         * Returns new instance of [TextLayoutFragment].
         *
         * @param text is a String with the card main text.
         * @param footer is a String with the card footer text.
         * @param timestamp is a String with the card timestamp text.
         */
        @JvmStatic
        fun newInstance(
            text: String,
            footer: String,
            timestamp: String,
            menu: Int?
        ): TextLayoutFragment = TextLayoutFragment().apply {
            val args = Bundle().apply {
                putString(TEXT_KEY, text)
                putString(FOOTER_KEY, footer)
                putString(TIMESTAMP_KEY, timestamp)
                menu?.let { putInt(MENU_KEY, it) }
            }
            setArguments(args)
        }
    }
}
