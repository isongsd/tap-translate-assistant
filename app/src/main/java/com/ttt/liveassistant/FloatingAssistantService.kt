package com.ttt.liveassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.ttt.liveassistant.translation.RemoteTranslator
import kotlin.math.abs

class FloatingAssistantService : Service() {
    private val remoteTranslator = RemoteTranslator()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var targetLayoutParams: WindowManager.LayoutParams
    private var overlayView: View? = null
    private var targetView: View? = null
    private var autoClickRunning = false
    private var autoClickEndsAtMs = 0L
    private var clickStatusView: TextView? = null
    private var compactClickButton: TextView? = null
    private val runAutoClick = object : Runnable {
        override fun run() {
            runAutoClickTick()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        activeService = this
        startForegroundCompat()
        showOverlay()
        showTargetView()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAutoClick("點按已停止", showToast = false)
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        targetView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        targetView = null
        if (activeService === this) {
            activeService = null
        }
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) {
            return
        }

        val content = buildOverlayView()
        layoutParams = WindowManager.LayoutParams(
            dp(330),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            OVERLAY_BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(14)
            y = dp(80)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        windowManager.addView(content, layoutParams)
        overlayView = content
    }

    private fun showTargetView() {
        if (targetView != null) {
            return
        }

        val target = CrosshairView(this)
        targetLayoutParams = WindowManager.LayoutParams(
            dp(56),
            dp(56),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            targetWindowFlags(touchable = true),
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(240)
        }

        attachTargetDragHandler(target)
        windowManager.addView(target, targetLayoutParams)
        targetView = target
    }

    private fun buildOverlayView(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBackground(Color.WHITE, dp(8), Color.rgb(210, 216, 228))
            elevation = dp(8).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        val title = TextView(this).apply {
            text = "點按翻譯助手"
            textSize = 16f
            setTextColor(Color.rgb(21, 25, 35))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val collapseButton = smallButton("收起")
        val closeButton = smallButton("關閉").apply {
            setOnClickListener { stopSelf() }
        }

        header.addView(title)
        header.addView(collapseButton)
        header.addView(closeButton)
        attachDragHandler(header)

        val quickInput = translateInput()
        val translateButton = compactIconButton("✓").apply {
            setOnClickListener { translateToClipboard(quickInput) }
        }
        val compactTapButton = compactIconButton("▶").apply {
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.rgb(34, 197, 94), dp(20), Color.TRANSPARENT)
            setOnClickListener { toggleAutoClick() }
        }
        compactClickButton = compactTapButton
        quickInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                translateToClipboard(quickInput)
                true
            } else {
                false
            }
        }

        val compactToggle = compactIconButton("譯")
        val compactDirection = TextView(this).apply {
            text = shortDirectionText()
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(86, 96, 112))
            setPadding(0, 0, 0, 0)
        }

        val compactBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            addView(compactToggle)
            addView(
                compactDirection,
                LinearLayout.LayoutParams(dp(54), dp(40)).apply {
                    marginStart = dp(6)
                },
            )
            addView(
                quickInput,
                LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                    marginStart = dp(6)
                    marginEnd = dp(8)
                },
            )
            addView(translateButton)
            addView(
                compactTapButton,
                LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    marginStart = dp(8)
                },
            )
        }

        val expandedDirection = TextView(this).apply {
            text = shortDirectionText()
            textSize = 18f
            setTextColor(Color.rgb(21, 25, 35))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(Color.rgb(248, 250, 252), dp(8), Color.rgb(210, 216, 228))
        }

        val expandedSuffix = TextView(this).apply {
            text = suffixText()
            textSize = 16f
            setTextColor(Color.rgb(21, 25, 35))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = roundedBackground(Color.rgb(248, 250, 252), dp(8), Color.rgb(210, 216, 228))
        }
        val tapStatus = body("點按輔助未啟動。拖曳綠色準心調整位置。")
        clickStatusView = tapStatus

        val expandedContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(label("翻譯方向"))
            addView(expandedDirection, matchWrap())
            addView(label("翻譯尾碼"))
            addView(expandedSuffix, matchWrap())
            addView(body("收起後可在小輸入列翻譯並複製；尾碼請於主畫面設定。"))
            addView(label("點按輔助"))
            addView(tapStatus, matchWrap())
            addView(body("收起後可用綠色 ▶ 開始，紅色 ■ 停止。"))
        }

        val scrollContent = ScrollView(this).apply {
            addView(expandedContent)
        }

        fun setCollapsed(shouldCollapse: Boolean) {
            releaseOverlayFocus()
            compactDirection.text = shortDirectionText()
            expandedDirection.text = shortDirectionText()
            expandedSuffix.text = suffixText()
            scrollContent.visibility = if (shouldCollapse) View.GONE else View.VISIBLE
            header.visibility = if (shouldCollapse) View.GONE else View.VISIBLE
            compactBar.visibility = if (shouldCollapse) View.VISIBLE else View.GONE
            container.setPadding(
                if (shouldCollapse) dp(6) else dp(10),
                if (shouldCollapse) dp(6) else dp(10),
                if (shouldCollapse) dp(6) else dp(10),
                if (shouldCollapse) dp(6) else dp(10),
            )
            layoutParams.width = if (shouldCollapse) dp(330) else dp(330)
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            windowManager.updateViewLayout(container, layoutParams)
        }

        collapseButton.setOnClickListener { setCollapsed(true) }
        attachDragOrClickHandler(compactToggle) { setCollapsed(false) }

        container.addView(header, matchWrap())
        container.addView(compactBar, matchWrap())
        container.addView(
            scrollContent,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            ),
        )

        return container
    }

    private fun translateInput(): EditText =
        EditText(this).apply {
            hint = "輸入要翻譯的文字"
            setSingleLine(true)
            maxLines = 1
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(dp(8), 0, dp(8), 0)
            minHeight = 0
            includeFontPadding = false
            background = roundedBackground(Color.rgb(248, 250, 252), dp(18), Color.rgb(210, 216, 228))
            enableOverlayTextInput(this)
        }

    private fun translateToClipboard(input: EditText) {
        val rawText = input.text.toString().trim()
        if (rawText.isBlank()) {
            toast("請先輸入要翻譯的文字")
            return
        }

        val source = AppSettings.sourceLanguage(this)
        val target = AppSettings.targetLanguage(this)
        val localText = ChineseTextConverter.convert(rawText, source, target)

        if (localText.isNotBlank() || source == target) {
            finishTranslation(input, localText.ifBlank { rawText })
            return
        }

        val endpoint = AppSettings.translationEndpoint(this)
        if (endpoint.isBlank()) {
            toast("此語言方向需要設定翻譯後端")
            return
        }

        hideKeyboard(input)
        releaseOverlayFocus()
        toast("翻譯中")

        Thread {
            val translated = runCatching {
                remoteTranslator.translate(endpoint, rawText, source, target)
            }.getOrNull().orEmpty()

            mainHandler.post {
                if (translated.isBlank()) {
                    toast("翻譯失敗，請確認後端設定")
                } else {
                    finishTranslation(input, translated)
                }
            }
        }.start()
    }

    private fun finishTranslation(input: EditText, translatedText: String) {
        hideKeyboard(input)
        releaseOverlayFocus()
        if (copyToClipboard(translatedText + AppSettings.formattedTranslationSuffix(this), showSuccessToast = false)) {
            input.text.clear()
            toast("已翻譯並複製")
        }
    }

    private fun copyToClipboard(text: String, showSuccessToast: Boolean = true): Boolean =
        runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("tap-translation", text))
        }.onSuccess {
            if (showSuccessToast) {
                toast("已複製到剪貼簿")
            }
        }.onFailure {
            toast("複製失敗")
        }.isSuccess

    private fun toggleAutoClick() {
        if (autoClickRunning) {
            stopAutoClick("點按已停止")
        } else {
            startAutoClick()
        }
    }

    private fun startAutoClick() {
        releaseOverlayFocus()

        if (!MonetizationProducts.DEBUG_TAP_ASSIST_UNLOCKED) {
            toast("點按輔助進階版需訂閱 NT$${MonetizationProducts.TAP_ASSIST_MONTHLY_PRICE_TWD}/月")
            updateClickUi("正式版需訂閱點按輔助進階版後才能啟用。")
            return
        }

        if (!TapAccessibilityService.isEnabled()) {
            toast("請到無障礙的「已安裝的應用程式」開啟點按輔助")
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            updateClickUi("尚未開啟無障礙點按輔助。請到「已安裝的應用程式」開啟本服務。")
            return
        }

        if (targetView == null) {
            showTargetView()
        }

        autoClickRunning = true
        autoClickEndsAtMs = SystemClock.elapsedRealtime() + AUTO_CLICK_DURATION_MS
        setTargetTouchable(false)
        updateClickUi()
        mainHandler.removeCallbacks(runAutoClick)
        mainHandler.post(runAutoClick)
        toast("點按已開始，最長 90 秒")
    }

    private fun runAutoClickTick() {
        if (!autoClickRunning) {
            return
        }

        val remainingMs = autoClickEndsAtMs - SystemClock.elapsedRealtime()
        if (remainingMs <= 0L) {
            stopAutoClick("90 秒已到，點按已停止")
            return
        }

        if (!TapAccessibilityService.isEnabled()) {
            stopAutoClick("無障礙點按輔助已關閉")
            return
        }

        val target = targetCenter()
        val dispatched = TapAccessibilityService.tap(target.x, target.y)
        if (!dispatched) {
            stopAutoClick("點按送出失敗，請重試")
            return
        }

        updateClickUi()
        mainHandler.postDelayed(runAutoClick, AUTO_CLICK_INTERVAL_MS)
    }

    private fun stopAutoClick(message: String, showToast: Boolean = true) {
        val wasRunning = autoClickRunning
        autoClickRunning = false
        autoClickEndsAtMs = 0L
        mainHandler.removeCallbacks(runAutoClick)
        setTargetTouchable(true)
        updateClickUi("點按輔助未啟動。拖曳綠色準心調整位置。")
        if (wasRunning && showToast) {
            toast(message)
        }
    }

    private fun targetCenter(): Point {
        val view = targetView ?: return Point(0, 0)
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Point(location[0] + view.width / 2, location[1] + view.height / 2)
    }

    private fun setTargetTouchable(touchable: Boolean) {
        val view = targetView ?: return
        targetLayoutParams.flags = targetWindowFlags(touchable)
        targetLayoutParams.alpha = if (touchable) 1.0f else 0.65f
        view.alpha = if (touchable) 1.0f else 0.65f
        runCatching { windowManager.updateViewLayout(view, targetLayoutParams) }
    }

    private fun targetWindowFlags(touchable: Boolean): Int {
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        return if (touchable) {
            baseFlags
        } else {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
    }

    private fun updateClickUi(message: String? = null) {
        val remainingMs = (autoClickEndsAtMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val remainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
        val status = message ?: if (autoClickRunning) {
            "點按中：每 400ms 一次，剩餘 ${remainingSeconds} 秒。"
        } else {
            "點按輔助未啟動。拖曳綠色準心調整位置。"
        }

        clickStatusView?.text = status
        compactClickButton?.apply {
            text = if (autoClickRunning) "■" else "▶"
            background = roundedBackground(
                if (autoClickRunning) Color.rgb(239, 68, 68) else Color.rgb(34, 197, 94),
                dp(20),
                Color.TRANSPARENT,
            )
        }
    }

    private fun shortDirectionText(): String {
        val source = AppSettings.sourceLanguage(this)
        val target = AppSettings.targetLanguage(this)
        return "${source.shortName} → ${target.shortName}"
    }

    private fun suffixText(): String {
        val suffix = AppSettings.translationSuffix(this)
        if (suffix.isBlank()) {
            return "未設定"
        }

        return if (AppSettings.addSpaceBeforeSuffix(this) && suffix.firstOrNull()?.isWhitespace() == false) {
            "空格 + $suffix"
        } else {
            suffix
        }
    }

    private fun attachDragHandler(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, layoutParams) }
                    true
                }
                else -> false
            }
        }
    }

    private fun attachTargetDragHandler(view: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = targetLayoutParams.x
                    initialY = targetLayoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    targetLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    targetLayoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    targetView?.let { windowManager.updateViewLayout(it, targetLayoutParams) }
                    true
                }
                else -> false
            }
        }
    }

    private fun attachDragOrClickHandler(view: View, onClick: () -> Unit) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaX) > dp(3) || abs(deltaY) > dp(3)) {
                        moved = true
                    }
                    layoutParams.x = initialX + deltaX.toInt()
                    layoutParams.y = initialY + deltaY.toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, layoutParams) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        onClick()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun enableOverlayTextInput(input: EditText) {
        input.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                stopAutoClick("已偵測到文字輸入，已停止點按")
                setOverlayFocusable(true)
                view.post {
                    input.requestFocus()
                    input.setSelection(input.text.length)
                    showKeyboard(input)
                }
            }
            false
        }
    }

    private fun releaseOverlayFocus() {
        overlayView?.let { view ->
            view.clearFocus()
            hideKeyboard(view)
        }
        setOverlayFocusable(false)
    }

    private fun setOverlayFocusable(focusable: Boolean) {
        val focusFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val newFlags = if (focusable) {
            OVERLAY_BASE_FLAGS
        } else {
            OVERLAY_BASE_FLAGS or focusFlag
        }

        if (layoutParams.flags == newFlags) {
            return
        }

        layoutParams.flags = newFlags
        overlayView?.let { view ->
            runCatching { windowManager.updateViewLayout(view, layoutParams) }
        }
    }

    private fun buildNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "點按翻譯助手",
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("點按翻譯助手執行中")
            .setContentText("懸浮窗提供翻譯與複製。")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.rgb(86, 96, 112))
        setPadding(0, dp(8), 0, dp(3))
    }

    private fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.rgb(86, 96, 112))
        setPadding(0, dp(8), 0, 0)
    }

    private fun smallButton(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(21, 25, 35))
        background = roundedBackground(Color.rgb(232, 236, 242), dp(18), Color.TRANSPARENT)
        setPadding(dp(8), 0, dp(8), 0)
        layoutParams = LinearLayout.LayoutParams(dp(54), dp(36))
    }

    private fun compactIconButton(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 13f
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(21, 25, 35))
        background = roundedBackground(Color.rgb(232, 236, 242), dp(20), Color.TRANSPARENT)
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
    }

    private fun roundedBackground(fillColor: Int, radius: Int, strokeColor: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fillColor)
            cornerRadius = radius.toFloat()
            setStroke(1, strokeColor)
        }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private class CrosshairView(context: Context) : View(context) {
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 197, 94)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 34, 197, 94)
            style = Paint.Style.FILL
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 25, 35)
            textAlign = Paint.Align.CENTER
            textSize = 28f
            strokeWidth = 2f
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (width.coerceAtMost(height) / 2f) - 4f

            canvas.drawCircle(centerX, centerY, radius, fillPaint)
            canvas.drawCircle(centerX, centerY, radius, strokePaint)
            canvas.drawLine(centerX, 4f, centerX, height - 4f, strokePaint)
            canvas.drawLine(4f, centerY, width - 4f, centerY, strokePaint)
            canvas.drawText("1", centerX, centerY + 9f, textPaint)
        }
    }

    companion object {
        const val CHANNEL_ID = "tap_translate_assistant"
        const val NOTIFICATION_ID = 1001
        const val AUTO_CLICK_INTERVAL_MS = 400L
        const val AUTO_CLICK_DURATION_MS = 90_000L
        @Volatile private var activeService: FloatingAssistantService? = null
        val OVERLAY_BASE_FLAGS: Int =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        fun stopClickingFromAccessibility() {
            val service = activeService ?: return
            service.mainHandler.post {
                service.stopAutoClick("已偵測到文字輸入，已停止點按")
            }
        }
    }
}
