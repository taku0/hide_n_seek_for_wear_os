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
import android.support.wearable.complications.ProviderInfoRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import org.tatapa.watchface.hidenseek.R

class BooleanConfigItem(
    val name: String,
    val iconEnabledResourceId: Int,
    val iconDisabledResourceId: Int,
    val sharedPreferenceKey: String
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
            return BooleanViewHolder(
                sharedPreferences,
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.config_list_boolean_item,
                        parent,
                        false
                    )
            )
        }
    }

    class BooleanViewHolder(
        private val sharedPreferences: SharedPreferences,
        view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener, ConfigurableViewHolder {
        private val mSwitch: Switch =
            view.findViewById(R.id.toggle_switch)

        private var mEnabledIconResourceId = 0
        private var mDisabledIconResourceId = 0

        private var mSharedPreferenceKey: String? = null

        init {
            view.setOnClickListener(this)
        }

        override fun configure(configItem: ConfigItem) {
            configure(configItem as BooleanConfigItem)
        }

        private fun configure(configItemType: BooleanConfigItem) {
            setIcons(
                configItemType.iconEnabledResourceId,
                configItemType.iconDisabledResourceId
            )
            setName(configItemType.name)
            setSharedPreferenceKey(configItemType.sharedPreferenceKey)
        }

        private fun setName(name: String) {
            mSwitch.text = name
        }

        private fun setIcons(
            enabledIconResourceId: Int,
            disabledIconResourceId: Int
        ) {
            mEnabledIconResourceId = enabledIconResourceId
            mDisabledIconResourceId = disabledIconResourceId

            val context = mSwitch.context

            // Set default to enabled.
            mSwitch.setCompoundDrawablesWithIntrinsicBounds(
                context.getDrawable(mEnabledIconResourceId),
                null,
                null,
                null
            )
        }

        private fun setSharedPreferenceKey(
            sharedPreferenceKey: String
        ) {
            mSharedPreferenceKey = sharedPreferenceKey

            updateIcon(
                mSwitch.context,
                sharedPreferences.getBoolean(sharedPreferenceKey, true)
            )
        }

        private fun updateIcon(context: Context, currentState: Boolean) {
            val currentIconResourceId = if (currentState) {
                mEnabledIconResourceId
            } else {
                mDisabledIconResourceId
            }

            mSwitch.isChecked = currentState
            mSwitch.setCompoundDrawablesWithIntrinsicBounds(
                context.getDrawable(currentIconResourceId),
                null,
                null,
                null
            )
        }

        override fun onClick(view: View) {
            val position = adapterPosition

            Log.d(TAG, "Complication onClick() position: $position")

            val context = view.context
            val newState = mSwitch.isChecked

            val editor = sharedPreferences.edit()

            editor.putBoolean(mSharedPreferenceKey, newState)
            editor.apply()

            updateIcon(context, newState)
        }

        companion object {
            private val TAG = BooleanViewHolder::class.java.simpleName
        }
    }
}
