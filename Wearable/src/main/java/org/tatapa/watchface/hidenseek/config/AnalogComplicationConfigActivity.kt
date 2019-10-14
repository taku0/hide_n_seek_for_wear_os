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
import android.content.Intent
import android.os.Bundle
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderChooserIntent
import android.util.Log

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.widget.WearableRecyclerView

import org.tatapa.watchface.hidenseek.R
import org.tatapa.watchface.hidenseek.watchface.AnalogComplicationWatchFaceService

/**
 * The watch-side config activity for [AnalogComplicationWatchFaceService],
 * which allows for setting complications of watch face along with the colors
 * of parts, and unread notifications toggle.
 */
class AnalogComplicationConfigActivity : Activity() {
    private lateinit var mAdapter: AnalogComplicationConfigRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_analog_complication_config)

        val wearableRecyclerView =
            findViewById<WearableRecyclerView>(R.id.wearable_recycler_view)

        wearableRecyclerView.layoutManager = LinearLayoutManager(this)

        // `isEdgeItemsCenteringEnabled` assumes that the top item and the
        // bottom item have same height.  In our case, it doesn't hold, so we
        // adjust manually.
        val bottomPadding =  resources.displayMetrics.heightPixels / 2

        wearableRecyclerView.setPadding(
            wearableRecyclerView.paddingLeft,
            wearableRecyclerView.paddingTop,
            wearableRecyclerView.paddingRight,
            bottomPadding
        )

        // Request focus for the sake of rotary input
        // https://developer.android.com/training/wearables/ui/rotary-input
        wearableRecyclerView.requestFocus()

        // Improves performance because we know changes in content do
        // not change the layout size of the RecyclerView.
        wearableRecyclerView.setHasFixedSize(true)

        val strikingIntervals =
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60)

        val strikingTimes: IntArray =
            (0..(24 * 60) step 15).toList().toIntArray()

        fun formatTime(minutesOfDay: Int): String {
            val formattedHours = (minutesOfDay / 60).toString()
            val formattedMinutes =
                (minutesOfDay % 60 + 100).toString().substring(1)

            return "${formattedHours}:${formattedMinutes}"
        }

        val items = listOf(
            PreviewAndComplicationsConfigItem(
                R.drawable.add_complication
            ),

            ColorConfigItem(
                getString(R.string.config_second_hand_color_label),
                R.drawable.ic_palette_white_24dp,
                getString(R.string.saved_second_hand_color)
            ),

            ColorConfigItem(
                getString(R.string.config_hour_numbers_color_label),
                R.drawable.ic_palette_white_24dp,
                getString(R.string.saved_hour_numbers_color)
            ),

            ColorConfigItem(
                getString(R.string.config_ticks_color_label),
                R.drawable.ic_palette_white_24dp,
                getString(R.string.saved_ticks_color)
            ),

            ColorConfigItem(
                getString(R.string.config_center_complication_color_label),
                R.drawable.ic_palette_white_24dp,
                getString(R.string.saved_center_complication_color)
            ),

            ColorConfigItem(
                getString(R.string.config_other_complications_color_label),
                R.drawable.ic_palette_white_24dp,
                getString(R.string.saved_other_complications_color)
            ),

            BooleanConfigItem(
                getString(R.string.config_unread_notifications_label),
                R.drawable.ic_notifications_white_24dp,
                R.drawable.ic_notifications_off_white_24dp,
                getString(R.string.saved_show_unread_notifications_pref)
            ),

            NumberListConfigItem(
                getString(R.string.config_striking_interval_label),
                R.drawable.ic_notifications_white_24dp,
                getString(R.string.saved_striking_interval_pref),
                strikingIntervals,
                strikingIntervals.map { number ->
                    if (number == 0) {
                        getString(R.string.config_striking_interval_none)
                    } else {
                        number.toString()
                    }
                }.toTypedArray()
            ),

            NumberListConfigItem(
                getString(R.string.config_striking_from_label),
                R.drawable.ic_notifications_white_24dp,
                getString(R.string.saved_striking_from_pref),
                strikingTimes,
                strikingTimes.map(::formatTime).toTypedArray()
            ),

            NumberListConfigItem(
                getString(R.string.config_striking_until_label),
                R.drawable.ic_notifications_white_24dp,
                getString(R.string.saved_striking_until_pref),
                strikingTimes,
                strikingTimes.map(::formatTime).toTypedArray()
            )
        )

        mAdapter = AnalogComplicationConfigRecyclerViewAdapter(
            applicationContext,
            items
        )

        wearableRecyclerView.adapter = mAdapter
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (
            requestCode == COMPLICATION_CONFIG_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            val complicationProviderInfo =
                data?.getParcelableExtra<ComplicationProviderInfo>(
                    ProviderChooserIntent.EXTRA_PROVIDER_INFO
                )

            Log.d(TAG, "Provider: $complicationProviderInfo")

            mAdapter.updateSelectedComplication(complicationProviderInfo)
        } else if (
            requestCode == UPDATE_COLORS_CONFIG_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            mAdapter.updatePreviewColors()
        } else if (
            requestCode == UPDATE_NUMBERS_CONFIG_REQUEST_CODE &&
            resultCode == RESULT_OK
        ) {
            mAdapter.updateItems()
        }
    }

    companion object {
        private val TAG =
            AnalogComplicationConfigActivity::class.java.simpleName

        internal const val COMPLICATION_CONFIG_REQUEST_CODE = 1001
        internal const val UPDATE_COLORS_CONFIG_REQUEST_CODE = 1002
        internal const val UPDATE_NUMBERS_CONFIG_REQUEST_CODE = 1003
    }
}
