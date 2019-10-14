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
import android.graphics.Color
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import org.tatapa.watchface.hidenseek.R
import org.tatapa.watchface.hidenseek.watchface.AnalogComplicationWatchFaceService

/**
 * Allows user to select color for something on the watch face (background,
 * highlight,etc.) and saves it to [android.content.SharedPreferences]
 * in RecyclerView.Adapter.
 */
class ColorSelectionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_color_selection_config)

        val sharedPreferenceKey =
            intent.getStringExtra(EXTRA_SHARED_PREFERENCE_KEY)!!

        // TODO move somewhere
        val colors = 0.until(16).reversed().map { i ->
            Color.rgb(i * 255 / 15, i * 255 / 15, i * 255 / 15)
        }

        val configAppearanceWearableRecyclerView =
            findViewById<WearableRecyclerView>(R.id.wearable_recycler_view)

        configAppearanceWearableRecyclerView.layoutManager =
            LinearLayoutManager(this)

        // `isEdgeItemsCenteringEnabled` calls `scrollToPosition` just before
        // displaying the views.  This cancels `scrollToPosition` called from
        // `onCreate`.  So set paddings manually.
        val verticalPadding =  resources.displayMetrics.heightPixels / 3

        configAppearanceWearableRecyclerView.setPadding(
            configAppearanceWearableRecyclerView.paddingLeft,
            verticalPadding,
            configAppearanceWearableRecyclerView.paddingRight,
            verticalPadding
        )

        // Request focus for the sake of rotary input
        // https://developer.android.com/training/wearables/ui/rotary-input
        configAppearanceWearableRecyclerView.requestFocus()

        // Improves performance because we know changes in content do not
        // change the layout size of the RecyclerView.
        configAppearanceWearableRecyclerView.setHasFixedSize(true)

        configAppearanceWearableRecyclerView.adapter =
            ColorSelectionRecyclerViewAdapter(sharedPreferenceKey, colors)

        val sharedPreferences = AnalogComplicationWatchFaceService
                .getSharedPreferences(this)

        val currentValue =
            sharedPreferences.getInt(sharedPreferenceKey, colors[0])

        val selectedPosition = colors.indexOf(currentValue)

        if (selectedPosition != -1) {
            configAppearanceWearableRecyclerView.forceLayout()

            configAppearanceWearableRecyclerView.scrollToPosition(
                selectedPosition
            )
        }
    }

    companion object {
        fun startActivityForResult(parentActivity: Activity,
                                   requestCode: Int,
                                   sharedPreferenceKey: String) {
            val launchIntent = Intent(
                parentActivity,
                ColorSelectionActivity::class.java
            )

            // Pass shared preference name to save color value to.
            launchIntent.putExtra(
                EXTRA_SHARED_PREFERENCE_KEY,
                sharedPreferenceKey
            )

            parentActivity.startActivityForResult(
                launchIntent,
                requestCode
            )
        }

        private const val EXTRA_SHARED_PREFERENCE_KEY =
            "org.tatapa.watchface.hidenseek.config.extra.EXTRA_SHARED_PREFERENCE_KEY"
    }
}
