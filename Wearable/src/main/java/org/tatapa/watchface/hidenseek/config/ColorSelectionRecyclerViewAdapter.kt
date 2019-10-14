/*
 * Copyright 2019 taku0
 * Copyright 2017 The Android Open Source Project (for original WatchFace sample code)
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
package org.tatapa.watchface.hidenseek.config

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import org.tatapa.watchface.hidenseek.R
import org.tatapa.watchface.hidenseek.watchface.AnalogComplicationWatchFaceService

/**
 * Provides a binding from color selection data set to views that are displayed
 * within [ColorSelectionActivity].
 * Color options change appearance for the item specified on the watch face.
 * Value is saved to a [SharedPreferences] value passed to the class.
 */
class ColorSelectionRecyclerViewAdapter internal constructor(
    private val mSharedPreferenceKey: String,
    private val mColorOptionsDataSet: List<Int>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder(): viewType: $viewType")

        return ColorViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.color_config_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int
    ) {
        Log.d(TAG, "Element $position set.")

        val color = mColorOptionsDataSet[position]
        val colorViewHolder = viewHolder as ColorViewHolder

        colorViewHolder.updateViews(color)
    }

    override fun getItemCount(): Int {
        return mColorOptionsDataSet.size
    }

    /**
     * Displays color options for an item on the watch face and saves value to
     * the SharedPreference associated with it.
     */
    inner class ColorViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val mButton: Button = view.findViewById(R.id.color)

        init {
            view.setOnClickListener(this)
        }

        fun updateViews(color: Int) {
            mButton.setBackgroundColor(color)

            val sharedPreferences = AnalogComplicationWatchFaceService
                    .getSharedPreferences(mButton.context)

            val isSelected = sharedPreferences.contains(mSharedPreferenceKey) &&
                 sharedPreferences.getInt(mSharedPreferenceKey, 0) == color

            if (isSelected) {
                mButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_selected_color_24dp,
                    0,
                    0,
                    0
                )
            } else {
                mButton.setCompoundDrawablesRelative(null, null, null, null)
            }
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            val color = mColorOptionsDataSet[position]

            Log.d(TAG, "Color: $color onClick() position: $position")

            val activity = view.context as Activity

            val sharedPreferences = AnalogComplicationWatchFaceService
                    .getSharedPreferences(activity)

            val editor = sharedPreferences.edit()

            editor.putInt(mSharedPreferenceKey, color)
            editor.apply()

            // Let's Complication Config Activity know there was an update
            // to colors.
            activity.setResult(Activity.RESULT_OK)

            activity.finish()
        }
    }

    companion object {
        private val TAG =
            ColorSelectionRecyclerViewAdapter::class.java.simpleName
    }
}
