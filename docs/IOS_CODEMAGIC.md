# iOS 與 Codemagic 建置

本專案的 iOS 版定位為「翻譯、尾碼、剪貼簿與廣告」工具，不提供 Android 才有的懸浮窗、跨 App 點按、連點、無障礙點按或點按輔助訂閱。

## 基本資料

- App 名稱：`點按翻譯助手`
- iOS Bundle ID：`com.ttt.liveassistant`
- 最低 iOS 版本：iOS 16
- 技術：SwiftUI、CocoaPods、Google Mobile Ads SDK
- Xcode project 由 XcodeGen 在 Codemagic runner 上產生。

## iOS AdMob

- iOS AdMob App ID：`ca-app-pub-2030886956293359~4244536374`
- iOS Banner Ad Unit ID：`ca-app-pub-2030886956293359/2914917493`
- iOS App Open Ad Unit ID：`ca-app-pub-2030886956293359/7981435559`
- Debug build 使用 Google 官方 iOS 測試廣告單元。

`Info.plist` 已加入：

```text
GADApplicationIdentifier = ca-app-pub-2030886956293359~4244536374
```

## 專案位置

```text
D:\projects\TTt\ios
```

重要檔案：

- `ios/project.yml`：XcodeGen 設定。
- `ios/Podfile`：Google Mobile Ads SDK 依賴。
- `ios/TapTranslateAssistant/`：SwiftUI 原始碼與 asset catalog。
- `codemagic.yaml`：Codemagic workflows。

## Codemagic workflows

`codemagic.yaml` 目前提供三個 workflow：

1. `ios-debug-simulator`
   - 產生 Xcode project。
   - 執行 `pod install`。
   - 使用 iOS Simulator SDK 建置 Debug。
   - 不需要 Apple 簽章。
   - 不會上傳到 App Store Connect。

2. `ios-release-archive`
   - 產生 Xcode project。
   - 執行 `pod install`。
   - 建立 Release archive。
   - 需要在 Codemagic 設定 Apple Developer signing。
   - 不會上傳到 App Store Connect。

3. `ios-app-store-connect`
   - 產生 Xcode project。
   - 執行 `pod install`。
   - 使用 App Store distribution signing 建立 signed IPA。
   - 將 IPA 上傳到 App Store Connect。
   - 目前只上傳 build，不自動送 TestFlight 審查，也不自動送 App Store 審查。

## App Store Connect 上傳前置設定

`ios-debug-simulator` 成功只代表程式可編譯，不代表已上傳到 App Store Connect。要讓 App Store Connect 看見 build，必須跑 `ios-app-store-connect`，且需要先完成下列設定。

### 1. 建立 App Store Connect App record

App Store Connect 必須先有 App record：

```text
Bundle ID: com.ttt.liveassistant
SKU: tap-translate-assistant-ios
```

### 2. 建立 App Store Connect API Key

到 App Store Connect：

```text
Users and Access -> Integrations -> App Store Connect API -> +
```

權限建議選：

```text
App Manager
```

下載 `.p8` key，並記下：

- Key ID
- Issuer ID

### 3. 在 Codemagic 新增環境變數群組

在 Codemagic App settings 的 Environment variables 建立 group：

```text
appstore_credentials
```

加入三個變數：

```text
APP_STORE_CONNECT_PRIVATE_KEY
APP_STORE_CONNECT_KEY_IDENTIFIER
APP_STORE_CONNECT_ISSUER_ID
```

`APP_STORE_CONNECT_PRIVATE_KEY` 請貼 `.p8` 檔案完整內容，三個都要設成 Secret。

### 4. 在 Codemagic 設定 iOS 簽章

到 Codemagic：

```text
Team settings -> codemagic.yaml settings -> Code signing identities
```

需要有：

- Apple Distribution certificate
- App Store provisioning profile，Bundle ID 必須是 `com.ttt.liveassistant`

可用 App Store Connect API Key 讓 Codemagic 產生或抓取。

## Codemagic 使用步驟

1. 將此專案上傳到 GitHub / GitLab / Bitbucket。
2. 在 Codemagic 新增 App，選擇這個 repository。
3. Codemagic 偵測到 `codemagic.yaml` 後，先跑 `ios-debug-simulator`。
4. Debug build 成功後，再設定 App Store Connect API Key、Apple Distribution certificate、App Store provisioning profile。
5. App Store Connect 建立 Bundle ID `com.ttt.liveassistant`。
6. 再跑 `ios-app-store-connect`。
7. 上傳完成後，App Store Connect 可能需要數分鐘處理 build，請到 TestFlight 或 Activity 查看。

## 功能範圍

iOS 首版包含：

- 來源語言與目標語言設定。
- 本機繁簡轉換。
- 多國語翻譯後端呼叫，API 契約與 Android 共用 `POST /v1/translate`。
- 自訂翻譯尾碼，會原樣附加，不會 trim 掉空格。
- 翻譯後複製到剪貼簿並清空輸入框。
- Banner 廣告與 App Open 廣告設定。

iOS 首版不包含：

- 懸浮窗。
- 連點、準心、點按輔助。
- 讀取第三方 App 內容。
- 自動貼上或自動送出。
- 點按輔助月訂閱。
