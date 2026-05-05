package com.ttt.liveassistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            val className = event.className?.toString().orEmpty()
            if (className.contains("EditText", ignoreCase = true) ||
                className.contains("TextInput", ignoreCase = true)
            ) {
                FloatingAssistantService.stopClickingFromAccessibility()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun dispatchTap(x: Int, y: Int): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            lineTo(x.toFloat(), y.toFloat() + 1f)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, TAP_DURATION_MS))
            .build()

        return dispatchGesture(gesture, null, mainHandler)
    }

    companion object {
        private const val TAP_DURATION_MS = 45L
        private val mainHandler = Handler(Looper.getMainLooper())
        @Volatile private var instance: TapAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null

        fun tap(x: Int, y: Int): Boolean =
            instance?.dispatchTap(x, y) ?: false
    }
}
