package com.jarves.mh.spen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Samsung S Pen Ecosystem Controller for MindSparQ JARVIS.
 * Bridges S Pen hardware events (Detachment, Remote BLE Button, Hover, Pressure, Tilt)
 * directly into the Live Duplex Voice Session and Workspace Controls.
 */
class SPenVoiceController(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job()),
    private val onToggleListening: () -> Unit = {},
    private val onPttStart: () -> Unit = {},
    private val onPttEnd: () -> Unit = {},
    private val onBargeIn: () -> Unit = {},
    private val onStatusChange: (String) -> Unit = {},
) {
    companion object {
        private const val TAG = "SPenVoiceController"
        private const val ACTION_SAMSUNG_PEN_INSERT = "com.samsung.pen.INSERT"
        private const val ACTION_SEC_PEN_INSERT = "com.sec.android.intent.action.PEN_INSERTION_CHANGED"
        private const val EXTRA_PEN_INSERT = "penInsert"
        private const val DOUBLE_CLICK_WINDOW_MS = 380L
        private const val PTT_HOLD_THRESHOLD_MS = 300L
        // Keycode 308 corresponds to KEYCODE_STYLUS_BUTTON_PRIMARY in Android SDK
        private const val KEYCODE_STYLUS_BUTTON_PRIMARY = 308
        private const val KEYCODE_STYLUS_BUTTON_SECONDARY = 309
    }

    data class SPenState(
        val isDetached: Boolean = false,
        val isButtonPressed: Boolean = false,
        val isHovering: Boolean = false,
        val hoverX: Float = 0f,
        val hoverY: Float = 0f,
        val hoverDistance: Float = 0f,
        val tilt: Float = 0f,
        val pressure: Float = 0f,
        val lastGesture: String = "IDLE",
    )

    private val _state = MutableStateFlow(SPenState())
    val state: StateFlow<SPenState> = _state.asStateFlow()

    private var buttonDownTimestamp = 0L
    private var lastClickTimestamp = 0L
    private var isHoldingPtt = false
    private var clickPendingJob: Job? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val penInsertionReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == ACTION_SAMSUNG_PEN_INSERT || action == ACTION_SEC_PEN_INSERT) {
                // penInsert == true means the pen is INSERTED into the slot
                // penInsert == false means the pen is DETACHED/REMOVED
                val isInserted = intent.getBooleanExtra(EXTRA_PEN_INSERT, false)
                val isDetached = !isInserted
                Log.d(TAG, "S Pen insertion changed: inserted=$isInserted, detached=$isDetached")
                
                _state.value = _state.value.copy(
                    isDetached = isDetached,
                    lastGesture = if (isDetached) "PEN_DETACHED" else "PEN_INSERTED"
                )

                if (isDetached) {
                    performHaptic(40)
                    onStatusChange("🖊️ S Pen Active · Remote Push-To-Talk Ready")
                } else {
                    performHaptic(20)
                    onStatusChange("🖊️ S Pen Docked")
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(ACTION_SAMSUNG_PEN_INSERT)
            addAction(ACTION_SEC_PEN_INSERT)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(penInsertionReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(penInsertionReceiver, filter)
            }
            Log.d(TAG, "Registered Samsung S Pen broadcast receiver successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register S Pen broadcast receiver: ${e.message}")
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(penInsertionReceiver)
        } catch (_: Exception) {
        }
    }

    /**
     * Intercepts S Pen Remote BLE and Stylus Key Events.
     * Maps hardware clicks to:
     * - Single Click: Toggle Listen / Mute
     * - Double Click: Immediate Barge-in (Stop AI talking)
     * - Long Press: Push-To-Talk (Hold to talk, release to send)
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isSPenKey = keyCode == KEYCODE_STYLUS_BUTTON_PRIMARY ||
                keyCode == KEYCODE_STYLUS_BUTTON_SECONDARY ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP

        if (!isSPenKey) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) {
                    buttonDownTimestamp = System.currentTimeMillis()
                    _state.value = _state.value.copy(isButtonPressed = true)

                    // Schedule PTT detection if held beyond threshold
                    scope.launch {
                        delay(PTT_HOLD_THRESHOLD_MS)
                        if (_state.value.isButtonPressed && !isHoldingPtt) {
                            isHoldingPtt = true
                            performHaptic(50)
                            Log.d(TAG, "S Pen held down -> activating Push-to-Talk!")
                            onPttStart()
                        }
                    }
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                val now = System.currentTimeMillis()
                val pressDuration = now - buttonDownTimestamp
                _state.value = _state.value.copy(isButtonPressed = false)

                if (isHoldingPtt) {
                    // Released after long hold -> End PTT
                    isHoldingPtt = false
                    performHaptic(30)
                    Log.d(TAG, "S Pen released after hold -> finishing Push-to-Talk")
                    onPttEnd()
                    return true
                }

                // Short click handling (< 300ms)
                if (pressDuration < PTT_HOLD_THRESHOLD_MS) {
                    val timeSinceLastClick = now - lastClickTimestamp
                    if (timeSinceLastClick < DOUBLE_CLICK_WINDOW_MS) {
                        // Double click detected!
                        clickPendingJob?.cancel()
                        lastClickTimestamp = 0L
                        performHaptic(60)
                        Log.d(TAG, "S Pen Double Click -> Barge-in / Interrupt Speech!")
                        onBargeIn()
                        _state.value = _state.value.copy(lastGesture = "DOUBLE_CLICK_BARGE_IN")
                    } else {
                        // Potential single click -> wait for double click window
                        lastClickTimestamp = now
                        clickPendingJob?.cancel()
                        clickPendingJob = scope.launch {
                            delay(DOUBLE_CLICK_WINDOW_MS)
                            if (lastClickTimestamp == now) {
                                performHaptic(35)
                                Log.d(TAG, "S Pen Single Click -> Toggle Listening!")
                                onToggleListening()
                                _state.value = _state.value.copy(lastGesture = "SINGLE_CLICK_TOGGLE")
                            }
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * Intercepts Stylus MotionEvents to track Hover, Distance, Tilt, and Pressure.
     */
    fun handleMotionEvent(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        val isStylus = toolType == MotionEvent.TOOL_TYPE_STYLUS || toolType == MotionEvent.TOOL_TYPE_ERASER
        if (!isStylus) return false

        val isHover = event.action == MotionEvent.ACTION_HOVER_MOVE ||
                event.action == MotionEvent.ACTION_HOVER_ENTER ||
                event.action == MotionEvent.ACTION_HOVER_EXIT

        val isPrimaryBtn = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

        val distance = runCatching { event.getAxisValue(MotionEvent.AXIS_DISTANCE) }.getOrDefault(0f)
        val tilt = runCatching { event.getAxisValue(MotionEvent.AXIS_TILT) }.getOrDefault(0f)
        val pressure = event.pressure

        _state.value = _state.value.copy(
            isHovering = event.action != MotionEvent.ACTION_HOVER_EXIT && (isHover || distance > 0f),
            hoverX = event.x,
            hoverY = event.y,
            hoverDistance = distance,
            tilt = tilt,
            pressure = pressure,
            isButtonPressed = isPrimaryBtn,
        )

        return true
    }

    private fun performHaptic(durationMs: Long) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }
    }
}
