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
import android.content.Context
import android.content.SharedPreferences
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import org.tatapa.watchface.hidenseek.R

class NumberListConfigItem(
    val name: String,
    val iconResourceId: Int,
    val sharedPreferenceKey: String,
    val numbers: IntArray,
    val labels: Array<String>
) : ConfigItem {
    override val itemType get() = Companion

    companion object : ConfigItem.ConfigItemType {
        override val itemViewType = View.generateViewId()

        override fun createViewHolder(
            context: Context,
            sharedPreferences: SharedPreferences,
            providerInfoRetriever: ProviderInfoRetriever,
            parent: ViewGroup
        ): RecyclerView.ViewHolder {
            return NumberListViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.config_list_color_item, parent, false)
            )
        }
    }

    class NumberListViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view),
        View.OnClickListener,
        ConfigurableViewHolder {

        private val mButton: Button =
            view.findViewById(R.id.color_picker_button)

        private var mSharedPreferenceKey: String? = null
        private var mNumbers: IntArray = intArrayOf()
        private var mLabels: Array<String> = arrayOf()

        init {
            view.setOnClickListener(this)
        }

        override fun configure(configItem: ConfigItem) {
            configure(configItem as NumberListConfigItem)
        }

        private fun configure(configItemType: NumberListConfigItem) {
            setIcon(configItemType.iconResourceId)
            setName(configItemType.name)
            setSharedPreferenceKey(configItemType.sharedPreferenceKey)
            setNumbers(configItemType.numbers)
            setLabels(configItemType.labels)
        }

        private fun setName(name: String) {
            mButton.text = name
        }

        private fun setIcon(resourceId: Int) {
            val context = mButton.context

            mButton.setCompoundDrawablesWithIntrinsicBounds(
                context.getDrawable(resourceId),
                null,
                null,
                null
            )
        }

        private fun setSharedPreferenceKey(sharedPreferenceKey: String) {
            mSharedPreferenceKey = sharedPreferenceKey
        }

        private fun setNumbers(numbers: IntArray) {
            mNumbers = numbers
        }

        private fun setLabels(labels: Array<String>) {
            mLabels = labels
        }

        override fun onClick(view: View) {
            val position = adapterPosition
            Log.d(TAG, "Complication onClick() position: $position")

            NumberSelectionActivity.startActivityForResult(
                view.context as Activity,
                AnalogComplicationConfigActivity.UPDATE_NUMBERS_CONFIG_REQUEST_CODE,
                mSharedPreferenceKey!!,
                mNumbers,
                mLabels
            )
        }

        companion object {
            private val TAG = NumberListViewHolder::class.java.simpleName
        }
    }
}
