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

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.damn.anotherglass.glass.ee.host.R

/**
 * Fragment with the two column layout.
 */
class ColumnLayoutFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.left_column_layout, container, false)

        arguments?.let { args ->
            val imageView = ImageView(activity)
            imageView.setPadding(IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING, IMAGE_PADDING)
            imageView.setImageResource(args.getInt(IMAGE_KEY))
            val leftColumn = view.findViewById<FrameLayout>(R.id.left_column)
            leftColumn.addView(imageView)

            val textView = TextView(activity)
            textView.text = args.getString(TEXT_KEY)
            textView.textSize = TEXT_SIZE.toFloat()
            textView.setTypeface(Typeface.create(getString(R.string.thin_font), Typeface.NORMAL))

            val rightColumn = view.findViewById<FrameLayout>(R.id.right_column)
            rightColumn.addView(textView)

            val footer = view.findViewById<TextView>(R.id.footer)
            footer.text = args.getString(FOOTER_KEY, getString(R.string.empty_string))

            val timestamp = view.findViewById<TextView>(R.id.timestamp)
            timestamp.text = args.getString(TIMESTAMP_KEY, getString(R.string.empty_string))
        }
        return view
    }

    companion object {
        private const val IMAGE_KEY = "image_key"
        private const val TEXT_KEY = "text_key"
        private const val FOOTER_KEY = "footer_key"
        private const val TIMESTAMP_KEY = "timestamp_key"
        private const val TEXT_SIZE = 30
        private const val IMAGE_PADDING = 40

        /**
         * Returns new instance of [ColumnLayoutFragment].
         *
         * @param image is a android image resource to create a imageView on the left column.
         * @param text is a String with the card main text.
         * @param footer is a String with the card footer text.
         * @param timestamp is a String with the card timestamp text.
         */
        fun newInstance(
            image: Int,
            text: String?,
            footer: String?,
            timestamp: String?
        ): ColumnLayoutFragment = ColumnLayoutFragment().apply {
            val args = Bundle().apply {
                putInt(IMAGE_KEY, image) // todo: add Drawable support?
                putString(TEXT_KEY, text)
                putString(FOOTER_KEY, footer)
                putString(TIMESTAMP_KEY, timestamp)
            }
            setArguments(args)
        }
    }
}
