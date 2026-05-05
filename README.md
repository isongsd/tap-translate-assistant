# 點按翻譯助手

這個專案是 Android 版懸浮翻譯與點按輔助工具。它提供可停留在其他 App 上方的翻譯輸入列，使用者輸入文字後手動按確認，App 會依設定翻譯、複製到剪貼簿並清空輸入框。另可由使用者手動開啟無障礙點按輔助，在可見準心位置限時重複點按。

## 平台規劃

- Android 版：完整功能，包含懸浮翻譯、剪貼簿複製與限時點按輔助。
- iOS 版：只提供翻譯、尾碼、複製到剪貼簿等功能；不顯示點按輔助、綠色準心、開始/停止連點或點按輔助訂閱。
- 若未來開發 iOS 版，連點相關 UI 與付費項目需用平台判斷關閉，避免使用者購買無法在 iOS 使用的功能。

## 目前功能

- App 名稱：點按翻譯助手。
- 程式介面文字改為繁體中文。
- 可設定來源語言與目標語言。
- 可設定翻譯尾碼，例如直播字幕可在翻譯結果後附加 ` (13`。
- 支援本機繁體中文 / 簡體中文互轉，不需要網路或 API。
- 英文、日文、韓文、泰文、越南文、印尼文等多國語翻譯可透過雲端翻譯後端處理。
- Android 原生版提供懸浮窗，方便在其他 App 上方輸入、翻譯與複製。
- 懸浮窗展開面板只顯示設定狀態；文字輸入、翻譯與點按開始/停止集中在收起小列。
- 懸浮窗預設不搶輸入焦點，方便點擊其他 App 的原生輸入欄。
- 可選無障礙點按輔助：拖曳綠色準心設定位置，按開始後每 400ms 點按一次，最多 90 秒。
- 點按過程有可見停止鍵；偵測到文字輸入焦點時會停止。
- 不再包含彈幕 OCR、麥克風聽寫或直播回覆建議流程。
- 上架收益設計：免費版顯示 Google AdMob 廣告，Pro 永久版 NT$290 移除廣告，點按輔助進階版 NT$60/月。

## 專案結構

- `app/src/main/java/com/ttt/liveassistant/MainActivity.kt`：主設定畫面。
- `app/src/main/java/com/ttt/liveassistant/FloatingAssistantService.kt`：懸浮翻譯窗前景服務。
- `app/src/main/java/com/ttt/liveassistant/TapAccessibilityService.kt`：無障礙點按服務。
- `app/src/main/java/com/ttt/liveassistant/AppSettings.kt`：翻譯方向與後端設定。
- `app/src/main/java/com/ttt/liveassistant/ChineseTextConverter.kt`：本機繁簡轉換。
- `app/src/main/java/com/ttt/liveassistant/translation/RemoteTranslator.kt`：遠端翻譯 API 呼叫。
- `docs/ANDROID_OVERLAY.md`：Android 懸浮窗使用說明。
- `docs/COMPLIANCE.md`：合規邊界。
- `docs/FIREBASE_HOSTING.md`：Firebase Hosting、隱私權政策與 app-ads.txt 部署說明。
- `docs/IOS_CODEMAGIC.md`：iOS 版與 Codemagic 建置說明。
- `docs/MONETIZATION.md`：上架收益、廣告與內購設計。
- `docs/PLAY_CONSOLE.md`：Google Play Console 建立 App 與簽章指紋紀錄。
- `ios/`：SwiftUI iOS 版原始碼、XcodeGen 設定與 CocoaPods 設定。
- `server/llm_proxy.py`：OpenAI-compatible 翻譯後端範例。

## 圖示資產

- B 版 App icon 原始 ZIP 已保存於 `store-assets/icons/app-icon-b/app_icon_B_pack.zip`。
- 展開後的商店與平台圖示保存在 `store-assets/icons/app-icon-b/`。
- Android launcher 目前使用 ZIP 內的 `mipmap-mdpi-48.png`、`mipmap-hdpi-72.png`、`mipmap-xhdpi-96.png`、`mipmap-xxhdpi-144.png`、`mipmap-xxxhdpi-192.png`。
- Google Play 商品圖示可使用 `store-assets/icons/app-icon-b/icon-512.png`；`AppIcon-1024.png` 保留給 App Store 或高解析備用。
- Firebase Hosting 網站 favicon / apple-touch-icon 使用同一包圖示。

## 翻譯後端

主畫面可填入：

```text
https://你的後端/v1/translate
```

請勿把 OpenAI/Gemini API key 放進 Android APK。API key 應放在你自己的雲端後端環境變數中。

## 命令列打包與安裝

## iOS 與 Codemagic

iOS 版專案位於：

```text
D:\projects\TTt\ios
```

iOS Bundle ID 暫定並已寫入專案：

```text
com.ttt.liveassistant
```

Codemagic 設定檔位於：

```text
D:\projects\TTt\codemagic.yaml
```

先在 Codemagic 跑 `ios-debug-simulator`，確認 XcodeGen、CocoaPods 與 SwiftUI build 可通過；之後再設定 Apple Developer 簽章跑 `ios-release-archive`。

## Android 命令列打包與安裝

```powershell
.\scripts\build-debug-apk.ps1
```

生成的 APK：

```text
D:\projects\TTt\app\build\outputs\apk\debug\app-debug.apk
```

手機開啟 USB 偵錯並連接電腦後，可執行：

```powershell
.\scripts\install-debug-apk.ps1
```

Google Play Console 認領套件名稱時，請使用：

```powershell
.\scripts\build-play-claim-apk.ps1
```

Google Play Console 封閉測試版本若要求 AAB，請使用：

```powershell
.\scripts\create-release-keystore.ps1
.\scripts\build-play-release-aab.ps1
```
