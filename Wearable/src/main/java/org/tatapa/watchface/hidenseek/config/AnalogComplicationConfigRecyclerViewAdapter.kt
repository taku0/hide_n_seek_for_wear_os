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

import android.content.Context
import android.content.SharedPreferences
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.util.SparseArray
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import org.tatapa.watchface.hidenseek.config.ConfigItem.ConfigItemType
import org.tatapa.watchface.hidenseek.watchface.AnalogComplicationWatchFaceService

import java.util.concurrent.Executors

/**
 * Displays different layouts for configuring watch face's complications and
 * appearance settings (highlight color [second arm], background color,  show
 * unread notifications, etc.).
 *
 * All appearance settings are saved via [SharedPreferences].
 *
 * Layouts provided by this adapter are split into 5 main view types.
 *
 * A watch face preview including complications. Allows user to tap on the
 * complications to change the complication data and see a live preview of the
 * watch face.
 *
 * Simple arrow to indicate there are more options below the fold.
 *
 * Color configuration options for both highlight (seconds hand) and background
 * color.
 *
 * Toggle for show unread notifications.
 */
class AnalogComplicationConfigRecyclerViewAdapter(
    private val mContext: Context,
    private val mConfigItems: List<ConfigItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mSharedPreferences = AnalogComplicationWatchFaceService
            .getSharedPreferences(mContext)

    private val mProviderInfoRetriever = ProviderInfoRetriever(
        mContext,
        Executors.newCachedThreadPool()
    )

    private val mItemViewTypeToConfigItemType = SparseArray<ConfigItemType>()

    init {
        for (configItem in mConfigItems) {
            val configType = configItem.itemType

            mItemViewTypeToConfigItemType.put(
                configType.itemViewType,
                configType
            )
        }

        mProviderInfoRetriever.init()

        AnalogComplicationWatchFaceService
                .initializeSharedPreferences(mContext, mSharedPreferences)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder(): viewType: $viewType")

        return mItemViewTypeToConfigItemType
            .get(viewType)
            .createViewHolder(
                mContext,
                mSharedPreferences,
                mProviderInfoRetriever,
                parent
            )
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int
    ) {
        Log.d(TAG, "Element $position set.")

        if (viewHolder is ConfigurableViewHolder) {
            // Pulls all data required for creating the UX for the
            // specific setting option.
            viewHolder.configure(mConfigItems[position])
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is ViewAttachedToWindowListener) {
            holder.onViewAttachedToWindow()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return mConfigItems[position].itemType.itemViewType
    }

    override fun getItemCount(): Int {
        return mConfigItems.size
    }

    /**
     * Updates the selected complication id saved earlier with the new
     * information.
     */
    fun updateSelectedComplication(
        complicationProviderInfo: ComplicationProviderInfo?
    ) {
        Log.d(TAG, "updateSelectedComplication")

        mConfigItems.forEachIndexed { index, item ->
            if (item is PreviewAndComplicationsConfigItem) {
                item.selectedComplicationProviderInfo = complicationProviderInfo

                notifyItemChanged(index)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Required to release retriever for active complication data on detach.
        mProviderInfoRetriever.release()
    }

    fun updatePreviewColors() {
        Log.d(TAG, "updatePreviewColors()")

        mConfigItems.forEachIndexed { index, item ->
            if (item is PreviewAndComplicationsConfigItem) {
                notifyItemChanged(index)
            }
        }
    }

    fun updateItems() {
        Log.d(TAG, "updateItems()")

        notifyDataSetChanged()
    }

    companion object {
        private val TAG =
            AnalogComplicationConfigRecyclerViewAdapter::class.java.simpleName
    }
}
