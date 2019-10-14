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
package org.tatapa.watchface.hidenseek.watchface

import android.content.*
import android.graphics.*
import android.os.*
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.SystemProviders
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import org.tatapa.watchface.hidenseek.R
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogComplicationWatchFaceService : CanvasWatchFaceService() {
    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class UpdateTimeHandler(
            private val engineReference: WeakReference<Engine>
    ) : Handler() {
        override fun handleMessage(message: Message) {
            if (message.what != MSG_UPDATE_TIME) {
                return
            }

            val engine = engineReference.get() ?: return
            val delayMs = engine.handleTick()

            if (delayMs >= 0) {
                sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        internal fun start() {
            sendEmptyMessage(MSG_UPDATE_TIME)
        }

        internal fun stop() {
            removeMessages(MSG_UPDATE_TIME)
        }

        companion object {
            private const val MSG_UPDATE_TIME = 0
        }
    }

    data class WatchFaceColors(
        val handColor: Int = Color.WHITE,
        val secondHandColor: Int = Color.WHITE,
        val ticksColor: Int = Color.WHITE,
        val hourNumbersColor: Int = Color.WHITE,
        val centerComplicationColor: Int = Color.WHITE,
        val otherComplicationsColor: Int = Color.WHITE
    ) {
        constructor(
            context: Context,
            sharedPreferences: SharedPreferences
        ) : this(
            handColor = Color.WHITE,
            secondHandColor = loadColor(
                context,
                sharedPreferences,
                R.string.saved_second_hand_color
            ),
            ticksColor = loadColor(
                context,
                sharedPreferences,
                R.string.saved_ticks_color
            ),
            hourNumbersColor = loadColor(
                context,
                sharedPreferences,
                R.string.saved_hour_numbers_color
            ),
            centerComplicationColor = loadColor(
                context,
                sharedPreferences,
                R.string.saved_center_complication_color
            ),
            otherComplicationsColor = loadColor(
                context,
                sharedPreferences,
                R.string.saved_other_complications_color
            )
        )

        val colorsForAmbientMode: WatchFaceColors get() {
            fun colorForAmbientMode(color: Int): Int {
                val luminance = (Color.luminance(color) * 255).toInt()

                return Color.rgb(luminance, luminance, luminance)
            }

            return WatchFaceColors(
                handColor = colorForAmbientMode(handColor),
                secondHandColor = colorForAmbientMode(secondHandColor),
                ticksColor = colorForAmbientMode(ticksColor),
                hourNumbersColor = colorForAmbientMode(hourNumbersColor),
                centerComplicationColor =
                    colorForAmbientMode(centerComplicationColor),
                otherComplicationsColor =
                    colorForAmbientMode(otherComplicationsColor)
            )
        }

        companion object {
            private fun loadColor(
                context: Context,
                sharedPreferences: SharedPreferences,
                key: Int,
                default: Int = Color.WHITE
            ): Int {
                return sharedPreferences.getInt(context.getString(key), default)
            }
        }
    }

    data class WatchFacePaints(
        val hourHandPaint: Paint,
        val secondHandPaint: Paint,
        val ticksPaint: Paint,
        val hourNumbersPaint: Paint,
        val unreadNotificationIndicatorPaint: Paint,
        val backgroundPaint: Paint
    ) {
        constructor(colors: WatchFaceColors, density: Float) : this(
            hourHandPaint = createStrokePaint(
                colors.handColor,
                HAND_STROKE_WIDTH * density
            ),
            secondHandPaint = createStrokePaint(
                colors.secondHandColor,
                SECOND_HAND_STROKE_WIDTH * density
            ),
            ticksPaint = createFillPaint(colors.ticksColor),
            unreadNotificationIndicatorPaint = createStrokePaint(
                colors.handColor,
                UNREAD_NOTIFICATION_INDICATOR_WIDTH * density
            ),
            hourNumbersPaint = Paint().apply {
                color = colors.hourNumbersColor
                isAntiAlias = true
                textAlign = Paint.Align.CENTER

                var typeface = Typeface.create("Roboto Light", Typeface.NORMAL)

                if (Build.VERSION.SDK_INT >= 26) {
                    typeface = Typeface.Builder("Roboto Light")
                        .setFallback("Roboto")
                        .setWeight(300)
                        .build()
                }

                this.typeface = typeface
            },
            backgroundPaint = createFillPaint(Color.BLACK)
        )

        fun updateColors(colors: WatchFaceColors) {
            hourHandPaint.color = colors.handColor
            secondHandPaint.color = colors.secondHandColor
            ticksPaint.color = colors.ticksColor
            unreadNotificationIndicatorPaint.color = colors.handColor
            hourNumbersPaint.color = colors.hourNumbersColor
        }

        private val paints: Array<Paint> get() = arrayOf(
            hourHandPaint,
            secondHandPaint,
            ticksPaint,
            unreadNotificationIndicatorPaint,
            hourNumbersPaint
        )

        fun updateAntiAlias(isAntiAlias: Boolean) {
            for (paint in paints) {
                paint.isAntiAlias = isAntiAlias
            }
        }

        fun updateAlpha(alpha: Int) {
            for (paint in paints) {
                paint.alpha = alpha
            }
        }

        companion object {
            private const val HAND_STROKE_WIDTH = 1f
            private const val SECOND_HAND_STROKE_WIDTH = 1f
            private const val UNREAD_NOTIFICATION_INDICATOR_WIDTH = 1f

            private fun createStrokePaint(
                color: Int,
                strokeWidthPx: Float
            ): Paint {
                val paint = Paint()

                paint.color = color
                paint.strokeWidth = strokeWidthPx
                paint.isAntiAlias = true
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                paint.style = Paint.Style.STROKE

                return paint
            }

            private fun createFillPaint(color: Int): Paint {
                val paint = Paint()

                paint.color = color
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL

                return paint
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
        private lateinit var mCalendar: Calendar
        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode = false

        private var mStrikingStart = 7 * 60
        private var mStrikingEnd = 23 * 60
        private var mStrikingInterval = 0
        private var mLastStrikingTime = 0
        private var mStrikingDuration = 100L

        private var mCenterX = 0f
        private var mCenterY = 0f

        private var mColors: WatchFaceColors = WatchFaceColors()
        private lateinit var mPaints: WatchFacePaints

        // Maps active complication ids to the data for that
        // complication. Note: Data will only be present if the user
        // has chosen a provider via the settings activity for the
        // watch face.
        private lateinit var mActiveComplicationDataSparseArray:
                SparseArray<ComplicationData>

        private var mWideComplicationWidth = 44
        private var mComplicationSize = 44

        // Maps complication ids to corresponding ComplicationDrawable
        // that renders the the complication data on the watch face.
        private lateinit var mComplicationDrawableSparseArray:
                SparseArray<ComplicationDrawable>

        private var mLowBitAmbient = false
        private var mBurnInProtection = false

        private lateinit var mSharedPreferences: SharedPreferences

        // User's preference for if they want visual shown to indicate
        // unread notifications.
        private var mShowUnreadNotificationsPreference = false

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        // Handler to update the time once a second in interactive mode.
        private val mUpdateTimeHandler = UpdateTimeHandler(WeakReference(this))

        fun handleTick(): Long {
            invalidate()

            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()

                return INTERACTIVE_UPDATE_RATE_MS -
                    timeMs % INTERACTIVE_UPDATE_RATE_MS
            }

            return -1
        }

        override fun onCreate(holder: SurfaceHolder) {
            Log.d(TAG, "onCreate")

            super.onCreate(holder)

            // Used throughout watch face to pull user's preferences.
            val context = applicationContext

            mSharedPreferences = getSharedPreferences(context)

            mCalendar = Calendar.getInstance()

            initializeSharedPreferences(context, mSharedPreferences)
            loadSavedPreferences()
            initializeWatchFace()
        }

        private fun loadSavedPreferences() {
            mColors = WatchFaceColors(applicationContext, mSharedPreferences)

            val showUnreadNotificationPreferenceResourceName =
                applicationContext
                .getString(R.string.saved_show_unread_notifications_pref)

            mShowUnreadNotificationsPreference =
                mSharedPreferences.getBoolean(
                    showUnreadNotificationPreferenceResourceName,
                    true
                )

            mStrikingInterval = mSharedPreferences.getInt(
                applicationContext
                    .getString(R.string.saved_striking_interval_pref),
                0
            )

            mStrikingStart = mSharedPreferences.getInt(
                applicationContext
                    .getString(R.string.saved_striking_from_pref),
                0
            )

            mStrikingEnd = mSharedPreferences.getInt(
                applicationContext
                    .getString(R.string.saved_striking_until_pref),
                0
            )
        }

        private fun initializeWatchFace() {
            setWatchFaceStyle(
                WatchFaceStyle
                    .Builder(this@AnalogComplicationWatchFaceService)
                    .setAcceptsTapEvents(true)
                    .setHideNotificationIndicator(true)
                    .build()
            )

            mActiveComplicationDataSparseArray =
                SparseArray(COMPLICATION_IDS.size)

            mComplicationDrawableSparseArray =
                SparseArray(COMPLICATION_IDS.size)

            for (id in COMPLICATION_IDS) {
                mComplicationDrawableSparseArray.put(
                    id,
                    ComplicationDrawable(applicationContext)
                )
            }

            updateComplicationsStyles()
            setActiveComplications(*COMPLICATION_IDS)

            setDefaultSystemComplicationProvider(
                CENTER_COMPLICATION_ID,
                SystemProviders.DATE,
                ComplicationData.TYPE_SHORT_TEXT
            )

            mPaints = WatchFacePaints(mColors, resources.displayMetrics.density)
        }

        private fun updateComplicationsStyles() {
            val density = resources.displayMetrics.density

            for (id in COMPLICATION_IDS) {
                val complicationDrawable =
                    mComplicationDrawableSparseArray.get(id)

                val color = (
                    if (id == CENTER_COMPLICATION_ID)
                      mColors.centerComplicationColor
                    else
                      mColors.otherComplicationsColor
                )

                val luminance = (Color.luminance(color) * 255).toInt()
                val ambientColor = Color.rgb(luminance, luminance, luminance)

                // Active mode colors.
                complicationDrawable.setBorderColorActive(Color.TRANSPARENT)
                complicationDrawable.setRangedValuePrimaryColorActive(color)
                complicationDrawable.setHighlightColorActive(Color.WHITE)
                complicationDrawable.setIconColorActive(color)
                complicationDrawable.setTextColorActive(color)
                complicationDrawable.setTitleColorActive(color)
                complicationDrawable.setRangedValueRingWidthActive((
                    RANGED_VALUE_RING_WIDTH_ACTIVE * density).toInt()
                )

                // Ambient mode colors.
                complicationDrawable.setBorderColorAmbient(Color.TRANSPARENT)
                complicationDrawable.setRangedValuePrimaryColorAmbient(
                    ambientColor
                )
                complicationDrawable.setHighlightColorAmbient(Color.WHITE)
                complicationDrawable.setIconColorAmbient(ambientColor)
                complicationDrawable.setTextColorAmbient(ambientColor)
                complicationDrawable.setTitleColorAmbient(ambientColor)
                complicationDrawable.setRangedValueRingWidthAmbient(
                    (RANGED_VALUE_RING_WIDTH_AMBIENT * density).toInt()
                )
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.stop()

            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            Log.d(TAG, "onPropertiesChanged()")

            super.onPropertiesChanged(properties)

            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT,
                false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION,
                false
            )

            for (id in COMPLICATION_IDS) {
                val complicationDrawable =
                    mComplicationDrawableSparseArray.get(id)

                complicationDrawable.setLowBitAmbient(mLowBitAmbient)
                complicationDrawable.setBurnInProtection(mBurnInProtection)
            }
        }

        override fun onComplicationDataUpdate(
            complicationId: Int,
            complicationData: ComplicationData?
        ) {
            Log.d(TAG, "onComplicationDataUpdate() id: $complicationId")

            mActiveComplicationDataSparseArray
                .put(complicationId, complicationData)

            mComplicationDrawableSparseArray
                .get(complicationId)
                .setComplicationData(complicationData)

            invalidate()
        }

        override fun onTapCommand(
            tapType: Int,
            x: Int,
            y: Int,
            eventTime: Long
        ) {
            Log.d(TAG, "OnTapCommand()")

            if (tapType != WatchFaceService.TAP_TYPE_TAP) {
                return
            }

            for (id in COMPLICATION_IDS) {
                val successfulTap = mComplicationDrawableSparseArray
                    .get(id)
                    .onTap(x, y)

                if (successfulTap) {
                    return
                }
            }
        }

        override fun onTimeTick() {
            super.onTimeTick()

            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)

            Log.d(TAG, "onAmbientModeChanged: $inAmbientMode")

            updateWatchPaintStyles()

            for (complicationId in COMPLICATION_IDS) {
                mComplicationDrawableSparseArray
                    .get(complicationId)
                    .setInAmbientMode(inAmbientMode)
            }

            // If we go back to active mode, start timer.
            updateTimer()
        }

        private fun updateWatchPaintStyles() {
            mPaints.updateColors(
                if (isInAmbientMode)
                    mColors.colorsForAmbientMode
                else
                    mColors
            )
            mPaints.updateAntiAlias(!isInAmbientMode || !mLowBitAmbient)
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            Log.d(TAG, "onInterruptionFilterChanged(): $interruptionFilter")

            super.onInterruptionFilterChanged(interruptionFilter)

            val inMuteMode =
                interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            // Dim display in mute mode.
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode

                mPaints.updateAlpha(if (inMuteMode) 100 else 255)

                invalidate()
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            Log.d(TAG, "onSurfaceChanged()")

            super.onSurfaceChanged(holder, format, width, height)

            mCenterX = width / 2f
            mCenterY = height / 2f

            val radius = min(width / 2, height / 2)

            mComplicationSize = radius / 2
            mWideComplicationWidth = 4 * radius / 3
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()

            mCalendar.timeInMillis = now

            WatchFaceDrawer.drawWatchFace(
                canvas,
                bounds,
                resources.displayMetrics.density,
                mCalendar,
                isInAmbientMode,
                mComplicationDrawableSparseArray,
                mActiveComplicationDataSparseArray,
                mComplicationSize,
                mWideComplicationWidth,
                mShowUnreadNotificationsPreference &&
                    !mMuteMode &&
                    unreadCount > 0,
                mPaints
            )

            strikeIfNeeded()
        }

        private fun strikeIfNeeded() {
            val minutesOfTheDay =
                mCalendar.get(Calendar.HOUR_OF_DAY) * 60 +
            mCalendar.get(Calendar.MINUTE)

            if (
                minutesOfTheDay == mLastStrikingTime ||
                mStrikingInterval == 0 ||
                minutesOfTheDay % mStrikingInterval != 0
            ) {
                return
            }

            mLastStrikingTime = minutesOfTheDay

            if (minutesOfTheDay !in mStrikingStart until mStrikingEnd) {
                return
            }

            val vibrator =
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(
                            0,
                            mStrikingDuration,
                            mStrikingDuration,
                            mStrikingDuration,
                            mStrikingDuration,
                            mStrikingDuration
                        ),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    longArrayOf(
                        0,
                        mStrikingDuration,
                        mStrikingDuration,
                        mStrikingDuration,
                        mStrikingDuration,
                        mStrikingDuration
                    ),
                    -1
                )
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Log.d(TAG, "onVisibilityChanged(): $visible")

            super.onVisibilityChanged(visible)

            if (visible) {
                loadSavedPreferences()

                updateComplicationsStyles()
                updateWatchPaintStyles()

                registerReceiver()

                mCalendar.timeZone = TimeZone.getDefault()

                invalidate()
            } else {
                unregisterReceiver()
            }

            // If we go back to active mode, start timer.
            updateTimer()
        }

        override fun onUnreadCountChanged(count: Int) {
            Log.d(TAG, "onUnreadCountChanged(): $count")

            if (mShowUnreadNotificationsPreference && unreadCount != count) {
                invalidate()
            }
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }

            mRegisteredTimeZoneReceiver = true

            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)

            this@AnalogComplicationWatchFaceService
                .registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }

            mRegisteredTimeZoneReceiver = false

            this@AnalogComplicationWatchFaceService
                .unregisterReceiver(mTimeZoneReceiver)
        }

        private fun updateTimer() {
            mUpdateTimeHandler.stop()

            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.start()
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running.
         * The timer should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !isInAmbientMode
        }
    }

    companion object {
        private val TAG =
            AnalogComplicationWatchFaceService::class.java.simpleName

        // Unique IDs for each complication. The settings activity
        // that supports allowing users to select their complication
        // data provider requires numbers to be >= 0.
        const val CENTER_COMPLICATION_ID = 100
        const val TOP_COMPLICATION_ID = 101
        const val RIGHT_COMPLICATION_ID = 102
        const val BOTTOM_COMPLICATION_ID = 103
        const val LEFT_COMPLICATION_ID = 104

        // Complication IDs as array for Complication API.  Used by {@link
        // AnalogComplicationConfigRecyclerViewAdapter} to retrieve all
        // complication ids.
        val COMPLICATION_IDS = intArrayOf(
            CENTER_COMPLICATION_ID,
            TOP_COMPLICATION_ID,
            RIGHT_COMPLICATION_ID,
            BOTTOM_COMPLICATION_ID,
            LEFT_COMPLICATION_ID
        )

        private val COMPLICATION_SUPPORTED_TYPES = mapOf(
            CENTER_COMPLICATION_ID to
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
            ),

            TOP_COMPLICATION_ID to
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE,
                ComplicationData.TYPE_LONG_TEXT
            ),

            RIGHT_COMPLICATION_ID to
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
            ),

            BOTTOM_COMPLICATION_ID to
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE,
                ComplicationData.TYPE_LONG_TEXT
            ),

            LEFT_COMPLICATION_ID to
            intArrayOf(
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE
            )
        )

        // Used by {@link AnalogComplicationConfigRecyclerViewAdapter}
        // to see which complication types are supported in the settings
        // config activity.
        fun getSupportedComplicationTypes(id: Int): IntArray {
            return COMPLICATION_SUPPORTED_TYPES.getValue(id)
        }

        // Update rate in milliseconds for interactive mode. We update
        // once a second to advance the second hand.
        private val INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1)

        private const val RANGED_VALUE_RING_WIDTH_ACTIVE = 1.5f
        private const val RANGED_VALUE_RING_WIDTH_AMBIENT = 1.5f

        fun initializeSharedPreferences(
            context: Context,
            sharedPreferences: SharedPreferences
        ) {
            fun setBooleanDefault(id: Int, value: Boolean) {
                val key = context.getString(id)

                if (!sharedPreferences.contains(key)) {
                    sharedPreferences.edit().putBoolean(key, value).apply()
                }
            }

            fun setIntDefault(id: Int, value: Int) {
                val key = context.getString(id)

                if (!sharedPreferences.contains(key)) {
                    sharedPreferences.edit().putInt(key, value).apply()
                }
            }

            setBooleanDefault(
                R.string.saved_show_unread_notifications_pref,
                true
            )

            setIntDefault(
                R.string.saved_second_hand_color,
                Color.rgb(255, 255, 255)
            )
            setIntDefault(
                R.string.saved_ticks_color,
                Color.rgb(255, 255, 255)
            )
            setIntDefault(
                R.string.saved_hour_numbers_color,
                Color.rgb(187, 187, 187)
            )
            setIntDefault(
                R.string.saved_center_complication_color,
                Color.rgb(255, 255, 255)
            )
            setIntDefault(
                R.string.saved_other_complications_color,
                Color.rgb(136, 136, 136)
            )

            setIntDefault(R.string.saved_striking_interval_pref, 0)
            setIntDefault(R.string.saved_striking_from_pref, 7 * 60)
            setIntDefault(R.string.saved_striking_until_pref, 23 * 60)
        }

        fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                context.getString(
                    R.string.analog_complication_preference_file_key
                ),
                Context.MODE_PRIVATE
            )
        }
    }

    object WatchFaceDrawer {
        // https://developer.android.com/training/wearables/watch-faces/drawing#Screen
        private const val BURN_IN_PROTECTION_MARGIN = 10f

        private const val MAJOR_TICK_RADIUS = 1.25f
        private const val MINOR_TICK_RADIUS = 1f

        fun drawWatchFace(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            calendar: Calendar,
            isInAmbientMode: Boolean,
            complicationDrawableSparseArray:
                SparseArray<ComplicationDrawable>?,
            complicationDataSparseArray: SparseArray<ComplicationData>?,
            complicationSize: Int,
            wideComplicationWidth: Int,
            showUnreadNotifications: Boolean,
            paints: WatchFacePaints
        ) {
            drawBackground(canvas, bounds, paints)

            if (complicationDrawableSparseArray != null &&
                complicationDataSparseArray != null) {

                updateComplicationBounds(
                    complicationDrawableSparseArray,
                    bounds,
                    complicationSize,
                    wideComplicationWidth,
                    complicationDataSparseArray
                )

                drawComplications(
                    canvas,
                    complicationDrawableSparseArray,
                    calendar
                )
            }

            drawTicksAndHands(
                canvas,
                bounds,
                density,
                calendar,
                isInAmbientMode,
                paints
            )

            if (showUnreadNotifications) {
                drawUnreadNotificationIcon(canvas, bounds, density, paints)
            }

            if (complicationDrawableSparseArray != null &&
                complicationDataSparseArray != null) {

                drawCenterComplication(
                    canvas,
                    complicationDrawableSparseArray,
                    calendar
                )
            }
        }

        private fun updateComplicationBounds(
            complicationDrawableSparseArray:
                SparseArray<ComplicationDrawable>,
            canvasBounds: Rect,
            complicationSize: Int,
            wideComplicationWidth: Int,
            complicationDataSparseArray: SparseArray<ComplicationData>
        ) {
            val centerX = canvasBounds.centerX()
            val centerY = canvasBounds.centerY()
            val radius = min(centerX, centerY)

            val offsets = arrayOf(
                Triple(CENTER_COMPLICATION_ID, 0, 0),
                Triple(TOP_COMPLICATION_ID, 0, -1),
                Triple(RIGHT_COMPLICATION_ID, 1, 0),
                Triple(BOTTOM_COMPLICATION_ID, 0, 1),
                Triple(LEFT_COMPLICATION_ID, -1, 0)
            )

            for ((id, xOffset, yOffset) in offsets) {
                val x = centerX + xOffset * (radius / 2 + 4)
                val y = centerY + yOffset * (radius / 2 + 4)

                val complicationData = complicationDataSparseArray.get(id)

                val complicationWidth = (
                    if (complicationData?.type ==
                            ComplicationData.TYPE_LONG_TEXT)
                        wideComplicationWidth
                    else
                        complicationSize
                )

                val complicationHeight = complicationSize

                complicationDrawableSparseArray.get(id)?.bounds = Rect(
                    // Left, Top, Right, Bottom
                    x - complicationWidth / 2,
                    y - complicationHeight / 2,
                    x + complicationWidth / 2,
                    y + complicationHeight / 2
                )
            }
        }

        private fun drawBackground(
            canvas: Canvas,
            bounds: Rect,
            paints: WatchFacePaints
        ) {
            val radius =
                min(bounds.centerX(), bounds.centerY()) -
            BURN_IN_PROTECTION_MARGIN

            canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
            canvas.drawCircle(
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                radius,
                paints.backgroundPaint
            )
        }

        private fun drawCenterComplication(
            canvas: Canvas,
            complicationDrawableSparseArray:
                SparseArray<ComplicationDrawable>,
            calendar: Calendar
        ) {
            complicationDrawableSparseArray
                .get(CENTER_COMPLICATION_ID)
                ?.draw(canvas, calendar.timeInMillis)
        }

        private fun drawComplications(
            canvas: Canvas,
            complicationDrawableSparseArray:
                SparseArray<ComplicationDrawable>,
            calendar: Calendar
        ) {
            for (id in COMPLICATION_IDS) {
                if (id == CENTER_COMPLICATION_ID) {
                    continue
                }

                complicationDrawableSparseArray
                    .get(id)
                    ?.draw(canvas, calendar.timeInMillis)
            }
        }

        private fun drawUnreadNotificationIcon(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val radius = min(centerX, centerY) - BURN_IN_PROTECTION_MARGIN

            val path = Path()

            path.moveTo(
                centerX - 4f * density,
                centerY + radius - 2f * density
            )
            path.lineTo(
                centerX,
                centerY + radius
            )
            path.lineTo(
                centerX + 4f * density,
                centerY + radius - 2f * density
            )

            canvas.drawPath(path, paints.unreadNotificationIndicatorPaint)
        }

        private fun drawTicksAndHands(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            calendar: Calendar,
            isInAmbientMode: Boolean,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()
            val radius = min(centerX, centerY) - BURN_IN_PROTECTION_MARGIN

            val seconds = calendar.get(Calendar.SECOND)
            val minutes = calendar.get(Calendar.MINUTE)
            val hours = calendar.get(Calendar.HOUR_OF_DAY)

            // for debugging
            // val seconds = 0
            // val minutes = 0
            // val hours = 0

            // for debugging
            // val seconds = 0
            // val minutes = 0
            // val hours = 6

            // for debugging
            // val seconds = 37
            // val minutes = 8
            // val hours = 10

            // for debugging
            // canvas.drawColor(Color.GRAY)
            // canvas.drawCircle(
            //     mCenterX,
            //     mCenterY,
            //     radius,
            //     paints.backgroundPaint
            // )

            val hourTextSize =
                radius * Math.PI.toFloat() / 6f / density / 2f

            paints.hourNumbersPaint.textSize = hourTextSize * density

            val majorTickRadius = radius - hourTextSize * 0.75f * density
            val minorTickRadius = radius - hourTextSize * 0.1f * density

            drawTicks(
                canvas,
                bounds,
                density,
                hours,
                minutes,
                seconds,
                isInAmbientMode,
                hourTextSize,
                majorTickRadius,
                minorTickRadius,
                paints
            )

            val secondsLength = minorTickRadius - 2f * density

            // Ensure the "seconds" hand is drawn only when we are in
            // interactive mode.  Otherwise, we only update the watch
            // face once a minute.
            if (!isInAmbientMode) {
                drawSecondHand(
                    canvas,
                    bounds,
                    seconds,
                    secondsLength,
                    paints
                )
            }

            drawMinuteHand(
                canvas,
                bounds,
                density,
                minutes,
                secondsLength,
                paints
            )
            drawHourHand(
                canvas,
                bounds,
                density,
                hours,
                minutes,
                secondsLength,
                paints
            )
        }

        private fun drawTicks(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            hours: Int,
            minutes: Int,
            seconds: Int,
            isInAmbientMode: Boolean,
            hourTextSize: Float,
            majorTickRadius: Float,
            minorTickRadius: Float,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.centerX()
            val centerY = bounds.centerY()

            // Bitset representing which ticks should be drawn.
            var tickBitset = 0L

            tickBitset = updateTickBitset(tickBitset, hours * 5)
            tickBitset = updateTickBitset(tickBitset, (hours + 1) * 5)
            tickBitset = updateTickBitset(tickBitset, minutes)

            if (!isInAmbientMode) {
                tickBitset = updateTickBitset(tickBitset, seconds)
            }

            // for debugging
            // tickBitset = 0.inv()

            for (tickIndex in 0 until 60) {
                if ((tickBitset and (1L shl tickIndex)) == 0L) {
                    continue
                }

                val tickRot = (tickIndex * 2f * Math.PI.toFloat() / 60f)

                if (tickIndex % 5 == 0) {
                    val hourIndex = tickIndex / 5
                    val hourText =
                        (if (hourIndex == 0) 12 else hourIndex).toString()

                    val hourX = sin(tickRot) * majorTickRadius
                    val hourY = -cos(tickRot) * majorTickRadius

                    canvas.drawText(
                        hourText,
                        centerX + hourX,
                        centerY + hourY + hourTextSize / 2f,
                        paints.hourNumbersPaint
                    )
                }

                val tickX = sin(tickRot) * minorTickRadius
                val tickY = -cos(tickRot) * minorTickRadius

                val tickRadius = (
                    if (tickIndex % 5 == 0)
                        MAJOR_TICK_RADIUS
                    else
                        MINOR_TICK_RADIUS
                ) * density

                canvas.drawCircle(
                    centerX + tickX,
                    centerY + tickY,
                    tickRadius,
                    paints.ticksPaint
                )
            }
        }

        private fun updateTickBitset(bitset: Long, index: Int): Long {
            var updated = bitset

            fun wrap(index0: Int): Int {
                var wrapped = index0 % 60

                if (wrapped < 0) {
                    wrapped += 60
                }

                return wrapped
            }

            fun set(index0: Int) {
                updated = updated or (1L shl wrap(index0))
            }

            when (wrap(index) % 5) {
                0 -> set(index)
                1 -> {
                        set(index)
                        set(index - 1)
                }
                2 -> {
                        set(index)
                        set(index - 1)
                        set(index - 2)
                }
                3 -> {
                        set(index)
                        set(index + 1)
                        set(index + 2)
                }
                4 -> {
                        set(index)
                        set(index + 1)
                }
            }

            return updated
        }

        private fun drawSecondHand(
            canvas: Canvas,
            bounds: Rect,
            seconds: Int,
            secondsLength: Float,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val secondsRotation = seconds * 6f

            canvas.save()
            canvas.rotate(
                secondsRotation,
                centerX,
                centerY
            )
            canvas.drawLine(
                centerX,
                centerY,
                centerX,
                centerY - secondsLength,
                paints.secondHandPaint
            )
            canvas.restore()
        }

        private fun drawMinuteHand(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            minutes: Int,
            secondsLength: Float,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()

            // jumping minutes hand
            val minutesRotation = minutes * 6f

            val minutePath = Path()

            minutePath.moveTo(
                centerX - secondsLength * 0.1f,
                centerY + centerX / 4f + 3f * density
            )
            minutePath.lineTo(
                centerX - secondsLength * 0.15f,
                centerY - secondsLength * 0.75f
            )
            minutePath.lineTo(
                centerX,
                centerY - secondsLength * 0.90f
            )
            minutePath.lineTo(
                centerX + secondsLength * 0.15f,
                centerY - secondsLength * 0.75f
            )
            minutePath.lineTo(
                centerX + secondsLength * 0.1f,
                centerY + centerX / 4f + 3f * density
            )
            minutePath.close()

            canvas.save()
            canvas.rotate(minutesRotation, centerX, centerY)
            canvas.drawPath(minutePath, paints.backgroundPaint)
            canvas.drawPath(minutePath, paints.hourHandPaint)
            canvas.restore()
        }

        private fun drawHourHand(
            canvas: Canvas,
            bounds: Rect,
            density: Float,
            hours: Int,
            minutes: Int,
            secondsLength: Float,
            paints: WatchFacePaints
        ) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val hoursRotation = (hours + minutes / 60f) * 30f

            val centerCirclePath = Path()

            centerCirclePath.addCircle(
                centerX,
                centerY,
                centerX / 4f,
                Path.Direction.CW
            )

            val hourPath = Path()

            hourPath.moveTo(
                centerX - secondsLength * 0.12f,
                centerY + centerX / 4f + 3f * density
            )
            hourPath.lineTo(
                centerX - secondsLength * 0.2f,
                centerY - secondsLength * 0.5f
            )
            hourPath.lineTo(
                centerX,
                centerY - secondsLength * 0.7f
            )
            hourPath.lineTo(
                centerX + secondsLength * 0.2f,
                centerY - secondsLength * 0.5f
            )
            hourPath.lineTo(
                centerX + secondsLength * 0.12f,
                centerY + centerX / 4f + 3f * density
            )
            hourPath.close()

            hourPath.op(centerCirclePath, Path.Op.UNION)

            canvas.save()
            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawPath(hourPath, paints.backgroundPaint)
            canvas.drawPath(hourPath, paints.hourHandPaint)
            canvas.restore()
        }
    }
}

