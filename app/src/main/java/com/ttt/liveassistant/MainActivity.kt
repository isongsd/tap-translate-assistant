package com.ttt.liveassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : Activity() {
    private lateinit var translationEndpointInput: EditText
    private lateinit var translationSuffixInput: EditText
    private lateinit var translationSuffixSpaceCheckbox: CheckBox
    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private var startupInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContentView(buildContentView())
        loadStartupInterstitialAd()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        root.addView(title("點按翻譯助手"))
        root.addView(body("提供懸浮翻譯輸入列，轉換後複製到剪貼簿，由使用者自行貼上。"))
        root.addView(body("繁體/簡體可離線轉換；英文、日文等多國語翻譯需要設定雲端翻譯後端。"))
        root.addView(body("如需限時重複點按，請手動開啟無障礙點按輔助；點按開始後會有可見停止按鈕，最長 90 秒自動停止。"))

        val languages = TranslationLanguage.entries
        val languageNames = languages.map { it.displayName }

        sourceLanguageSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                languageNames,
            )
            setSelection(languages.indexOf(AppSettings.sourceLanguage(this@MainActivity)).coerceAtLeast(0))
        }
        root.addView(label("來源語言"))
        root.addView(sourceLanguageSpinner, matchWrap())

        targetLanguageSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                languageNames,
            )
            setSelection(languages.indexOf(AppSettings.targetLanguage(this@MainActivity)).coerceAtLeast(1))
        }
        root.addView(label("目標語言"))
        root.addView(targetLanguageSpinner, matchWrap())

        translationEndpointInput = EditText(this).apply {
            hint = "https://你的後端/v1/translate"
            setText(AppSettings.translationEndpoint(this@MainActivity))
            setSingleLine(true)
        }
        root.addView(label("翻譯後端（可選，多國語需要）"))
        root.addView(translationEndpointInput, matchWrap())

        translationSuffixInput = EditText(this).apply {
            hint = "例如：(13，可留空"
            setText(AppSettings.translationSuffix(this@MainActivity))
            setSingleLine(true)
        }
        translationSuffixSpaceCheckbox = CheckBox(this).apply {
            text = "尾碼前自動加入空格"
            textSize = 16f
            isChecked = AppSettings.addSpaceBeforeSuffix(this@MainActivity)
        }
        root.addView(label("翻譯尾碼（可選）"))
        root.addView(translationSuffixInput, matchWrap())
        root.addView(translationSuffixSpaceCheckbox, matchWrap())
        root.addView(body("例如尾碼填 (13 且勾選空格，輸出會是：翻譯結果 (13。"))
        root.addView(bannerAdView(), matchWrap())

        val saveButton = Button(this).apply {
            text = "儲存翻譯設定"
            setOnClickListener { saveTranslationSettings() }
        }
        root.addView(saveButton, matchWrap())

        val overlayButton = Button(this).apply {
            text = "啟動懸浮窗"
            setOnClickListener { startFloatingAssistantWithPermission() }
        }
        root.addView(overlayButton, matchWrap())

        val accessibilityButton = Button(this).apply {
            text = "開啟無障礙點按輔助"
            setOnClickListener { openAccessibilitySettings() }
        }
        root.addView(accessibilityButton, matchWrap())
        root.addView(body("進入無障礙後，點「已安裝的應用程式」→「點按翻譯助手點按輔助」→ 開啟。"))

        val appsSettingsButton = Button(this).apply {
            text = "開啟系統應用程式列表"
            setOnClickListener { openApplicationsSettings() }
        }
        root.addView(appsSettingsButton, matchWrap())
        root.addView(body("若系統拒絕授予無障礙存取權，請在列表中選擇「點按翻譯助手」，再點右上角「⋮」允許受限制設定。"))

        val stopOverlayButton = Button(this).apply {
            text = "停止懸浮窗"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingAssistantService::class.java))
                toast("已停止懸浮窗")
            }
        }
        root.addView(stopOverlayButton, matchWrap())

        root.addView(label("使用方式"))
        root.addView(body("啟動懸浮窗後，在小輸入列輸入文字並按確認，即會依設定翻譯、複製並清空輸入框。"))
        root.addView(body("拖曳綠色準心到目標位置，按綠色開始鍵即可每 400ms 點按一次，最多 90 秒。"))
        root.addView(body("本工具不會自動貼上、不會自動送出，也不讀取其他 App 的文字內容。"))

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun saveTranslationSettings() {
        val languages = TranslationLanguage.entries
        val source = languages[sourceLanguageSpinner.selectedItemPosition]
        val target = languages[targetLanguageSpinner.selectedItemPosition]

        if (source == target) {
            toast("來源與目標語言相同")
        }

        AppSettings.saveTranslationLanguages(this, source, target)
        AppSettings.saveTranslationEndpoint(this, translationEndpointInput.text.toString())
        AppSettings.saveTranslationSuffix(this, translationSuffixInput.text.toString())
        AppSettings.saveAddSpaceBeforeSuffix(this, translationSuffixSpaceCheckbox.isChecked)
        toast("翻譯設定已儲存")
    }

    private fun startFloatingAssistantWithPermission() {
        if (!Settings.canDrawOverlays(this)) {
            toast("請先允許顯示在其他應用程式上層")
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            startActivity(intent)
            return
        }

        requestNotificationPermissionIfNeeded()
        startForegroundService(Intent(this, FloatingAssistantService::class.java))
        toast("懸浮窗已啟動")
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        toast("請到「已安裝的應用程式」開啟點按輔助")
    }

    private fun openApplicationsSettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        toast("請選擇「點按翻譯助手」")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE,
        )
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 24f
        setPadding(0, 0, 0, dp(8))
    }

    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        setPadding(0, dp(16), 0, dp(6))
    }

    private fun body(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 16f
        setPadding(0, 0, 0, dp(8))
    }

    private fun bannerAdView(): AdView =
        AdView(this).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = if (isDebuggable()) {
                MonetizationProducts.TEST_BANNER_AD_UNIT_ID
            } else {
                MonetizationProducts.MAIN_BANNER_AD_UNIT_ID
            }
            loadAd(AdRequest.Builder().build())
        }

    private fun loadStartupInterstitialAd() {
        val adUnitId = if (isDebuggable()) {
            MonetizationProducts.TEST_INTERSTITIAL_AD_UNIT_ID
        } else {
            MonetizationProducts.STARTUP_INTERSTITIAL_AD_UNIT_ID
        }

        if (adUnitId.isBlank()) {
            return
        }

        InterstitialAd.load(
            this,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    startupInterstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            startupInterstitialAd = null
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                            startupInterstitialAd = null
                        }
                    }
                    if (!isFinishing && !isDestroyed) {
                        ad.show(this@MainActivity)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    startupInterstitialAd = null
                }
            },
        )
    }

    private fun isDebuggable(): Boolean =
        (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 2001
    }
}
