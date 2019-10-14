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
 * Provides a binding from number selection data set to views that are displayed
 * within [NumberSelectionActivity].
 * Number options change appearance for the item specified on the watch face.
 * Value is saved to a [SharedPreferences] value passed to the class.
 */
class NumberSelectionRecyclerViewAdapter internal constructor(
    private val mSharedPreferenceKey: String,
    private val mNumbers: IntArray,
    private val mLabels: Array<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // TODO merge with NumberSelectionRecyclerViewAdapter

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder(): viewType: $viewType")

        return NumberViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.number_config_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int
    ) {
        Log.d(TAG, "Element $position set.")

        val number = mNumbers[position]
        val label = mLabels[position]
        val numberViewHolder = viewHolder as NumberViewHolder

        numberViewHolder.updateViews(number, label)
    }

    override fun getItemCount(): Int {
        return mNumbers.size
    }

    /**
     * Displays number options for an item on the watch face and saves value to
     * the SharedPreference associated with it.
     */
    inner class NumberViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val mButton: Button = view.findViewById(R.id.button)

        init {
            view.setOnClickListener(this)
        }

        fun updateViews(number: Int, label: String) {
            mButton.text = label

            val sharedPreferences = AnalogComplicationWatchFaceService
                    .getSharedPreferences(mButton.context)

            val isSelected = sharedPreferences.contains(mSharedPreferenceKey) &&
                sharedPreferences.getInt(mSharedPreferenceKey, 0) == number

            val resourceId= if (isSelected) {
                R.color.list_item_selected_background
            } else {
                R.color.list_item_background
            }

            mButton.setBackgroundColor(mButton.context.getColor(resourceId))
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            val number = mNumbers[position]

            Log.d(TAG, "Number: $number onClick() position: $position")

            val activity = view.context as Activity
            val sharedPreferences = AnalogComplicationWatchFaceService
                    .getSharedPreferences(activity)
            val editor = sharedPreferences.edit()

            editor.putInt(mSharedPreferenceKey, number)
            editor.apply()

            // Let's Complication Config Activity know there was an update
            // to numbers.
            activity.setResult(Activity.RESULT_OK)

            activity.finish()
        }
    }

    companion object {
        private val TAG =
            NumberSelectionRecyclerViewAdapter::class.java.simpleName
    }
}
