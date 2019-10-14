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
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.wearable.complications.ComplicationHelperActivity
import android.support.wearable.complications.ComplicationProviderInfo
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import org.tatapa.watchface.hidenseek.R
import org.tatapa.watchface.hidenseek.watchface.AnalogComplicationWatchFaceService
import java.util.*
import kotlin.math.min

class PreviewAndComplicationsConfigItem(
    val defaultComplicationResourceId: Int
): ConfigItem {
    override val itemType get() = Companion

    var selectedComplicationProviderInfo: ComplicationProviderInfo? = null

    // Selected complication id by user.
    // Default value is invalid (only changed when user taps to
    // change complication).
    var selectedComplicationId = -1

    companion object : ConfigItem.ConfigItemType {
        override val itemViewType = View.generateViewId()
        override fun createViewHolder(
            context: Context,
            sharedPreferences: SharedPreferences,
            providerInfoRetriever: ProviderInfoRetriever,
            parent: ViewGroup
        ): RecyclerView.ViewHolder {
            return PreviewAndComplicationsViewHolder(
                context,
                sharedPreferences,
                providerInfoRetriever,
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.config_list_preview_and_complications_item,
                        parent,
                        false
                    )
            )
        }
    }

    class PreviewAndComplicationsViewHolder(
        private val context: Context,
        private val sharedPreferences: SharedPreferences,
        private val providerInfoRetriever: ProviderInfoRetriever,
        view: View
    ) : RecyclerView.ViewHolder(view),
        ConfigurableViewHolder,
        ViewAttachedToWindowListener {

        private val mWatchFaceBackgroundImageView: ImageView =
            view.findViewById(R.id.watch_face_background)

        private val mComplicationViewHolders = arrayOf(
            ComplicationViewHolder(
                AnalogComplicationWatchFaceService.CENTER_COMPLICATION_ID,
                view.findViewById(R.id.center_complication),
                view.findViewById(R.id.center_complication_background)
            ),

            ComplicationViewHolder(
                AnalogComplicationWatchFaceService.TOP_COMPLICATION_ID,
                view.findViewById(R.id.top_complication),
                view.findViewById(R.id.top_complication_background)
            ),

            ComplicationViewHolder(
                AnalogComplicationWatchFaceService.RIGHT_COMPLICATION_ID,
                view.findViewById(R.id.right_complication),
                view.findViewById(R.id.right_complication_background)
            ),

            ComplicationViewHolder(
                AnalogComplicationWatchFaceService.BOTTOM_COMPLICATION_ID,
                view.findViewById(R.id.bottom_complication),
                view.findViewById(R.id.bottom_complication_background)
            ),

            ComplicationViewHolder(
                AnalogComplicationWatchFaceService.LEFT_COMPLICATION_ID,
                view.findViewById(R.id.left_complication),
                view.findViewById(R.id.left_complication_background)
            )
        )

        private var mDefaultComplicationDrawable: Drawable? = null

        private var mConfigItem: PreviewAndComplicationsConfigItem? = null

        override fun configure(configItem: ConfigItem) {
            configure(configItem as PreviewAndComplicationsConfigItem)
        }

        private fun configure(
            configItem: PreviewAndComplicationsConfigItem
        ) {
            mConfigItem = configItem

            setDefaultComplicationDrawable(
                configItem.defaultComplicationResourceId
            )

            initializeComplications()

            updateWatchFaceColors()

            if (configItem.selectedComplicationId >= 0) {
                updateComplicationViews(
                    configItem.selectedComplicationId,
                    configItem.selectedComplicationProviderInfo
                )
            }
        }

        override fun onViewAttachedToWindow() {
            val metrics = itemView.context.resources.displayMetrics
            val size = min(metrics.heightPixels, metrics.widthPixels)

            itemView.layoutParams.width = size
            itemView.layoutParams.height = size

            itemView.forceLayout()

            updateWatchFaceColors()
        }

        private fun setDefaultComplicationDrawable(resourceId: Int) {
            val context = itemView.context

            mDefaultComplicationDrawable = context.getDrawable(resourceId)

            for (complicationViewHolders in mComplicationViewHolders) {
                complicationViewHolders.updateViews(null)
            }
        }

        internal fun updateComplicationViews(
            watchFaceComplicationId: Int,
            complicationProviderInfo: ComplicationProviderInfo?
        ) {
            Log.d(TAG, "updateComplicationViews(): id: $watchFaceComplicationId")
            Log.d(TAG, "\tinfo: $complicationProviderInfo")

            for (complicationViewHolder in mComplicationViewHolders) {
                if (watchFaceComplicationId == complicationViewHolder.id) {
                    complicationViewHolder.updateViews(complicationProviderInfo)
                }
            }
        }

        private fun initializeComplications() {
            val callback = object : ProviderInfoRetriever
                    .OnProviderInfoReceivedCallback() {
                override fun onProviderInfoReceived(
                    watchFaceComplicationId: Int,
                    complicationProviderInfo: ComplicationProviderInfo?
                ) {
                    Log.d(TAG, "onProviderInfoReceived: $complicationProviderInfo")

                    updateComplicationViews(
                        watchFaceComplicationId,
                        complicationProviderInfo
                    )
                }
            }

            val watchFaceComponentName = ComponentName(
                context,
                AnalogComplicationWatchFaceService::class.java
            )

            providerInfoRetriever.retrieveProviderInfo(
                callback,
                watchFaceComponentName,
                *AnalogComplicationWatchFaceService.COMPLICATION_IDS
            )
        }

        private fun updateWatchFaceColors() {
            val resources = itemView.context.resources
            val padding = resources.getDimensionPixelSize(
                R.dimen.watch_face_preview_background_padding
            )
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels - 2 * padding
            val height = displayMetrics.heightPixels - 2 * padding
            val density = displayMetrics.density

            val colors = AnalogComplicationWatchFaceService.WatchFaceColors(
                context,
                sharedPreferences
            )

            val paints = AnalogComplicationWatchFaceService.WatchFacePaints(
                colors,
                density
            )

            val bitmap = Bitmap.createBitmap(
                displayMetrics,
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            val bounds = Rect(0, 0, bitmap.width, bitmap.height)

            val calendar = Calendar.getInstance()

            calendar.set(Calendar.YEAR, 1970)
            calendar.set(Calendar.MONTH, 0)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 10)
            calendar.set(Calendar.MINUTE, 8)
            calendar.set(Calendar.SECOND, 37)

            AnalogComplicationWatchFaceService.WatchFaceDrawer.drawWatchFace(
                canvas,
                bounds,
                density,
                calendar,
                false,
                SparseArray(),
                SparseArray(),
                0,
                0,
                false,
                paints
            )

            mWatchFaceBackgroundImageView.setImageBitmap(bitmap)
        }

        inner class ComplicationViewHolder(
            val id: Int,
            private val button: ImageButton,
            private val background: ImageView
        ): View.OnClickListener {
            init {
                button.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                launchComplicationHelperActivity(view.context as Activity)
            }

            // Launches the helper class, so user can choose their complication
            // data provider.
            private fun launchComplicationHelperActivity(
                currentActivity: Activity
            ) {
                mConfigItem?.selectedComplicationId = id

                val supportedTypes = AnalogComplicationWatchFaceService
                        .getSupportedComplicationTypes(id)

                val watchFaceComponentName = ComponentName(
                    context,
                    AnalogComplicationWatchFaceService::class.java
                )

                currentActivity.startActivityForResult(
                    ComplicationHelperActivity
                            .createProviderChooserHelperIntent(
                                currentActivity,
                                watchFaceComponentName,
                                id,
                                *supportedTypes
                            ),
                    AnalogComplicationConfigActivity
                            .COMPLICATION_CONFIG_REQUEST_CODE
                )
            }

            fun updateViews(
                complicationProviderInfo: ComplicationProviderInfo?
            ) {
                if (complicationProviderInfo == null) {
                    button.setImageDrawable(mDefaultComplicationDrawable)
                    button.contentDescription = context.getString(
                        R.string.add_complication
                    )
                } else {
                    button.setImageIcon(complicationProviderInfo.providerIcon)
                    button.contentDescription = context.getString(
                        R.string.edit_complication,
                        "${complicationProviderInfo.appName} ${complicationProviderInfo.providerName}"
                    )
                }
            }
        }

        companion object {
            private val TAG = PreviewAndComplicationsViewHolder::class.java.simpleName
        }
    }
}
