# 收益方案

本專案上架 Android 商店時，建議採用「免費 + 廣告 + 付費移除廣告 + 連點訂閱」。

## 平台差異

- Android：可使用「免費 + 廣告 + Pro 永久版移除廣告 + 點按輔助進階版月訂閱」。
- iOS：不提供點按輔助，因此不顯示 `tap_assist_monthly` 訂閱；建議只保留免費廣告版與移除廣告的一次性內購。
- 跨平台帳號或官網文案需清楚標示點按輔助為 Android 限定功能。

## iOS AdMob 狀態

iOS 版已建立 AdMob App 與廣告單元。iOS 版需要使用下列 iOS 專用 ID，不能共用 Android 的 AdMob App ID 與廣告單元 ID。

1. Apple Bundle ID：`com.ttt.liveassistant`
2. AdMob iOS App ID：`ca-app-pub-2030886956293359~4244536374`
3. iOS Banner Ad Unit ID：`ca-app-pub-2030886956293359/2914917493`
4. iOS App open Ad Unit ID：`ca-app-pub-2030886956293359/7981435559`
5. iOS `Info.plist`：需加入 `GADApplicationIdentifier`，並依 Google Mobile Ads SDK 文件加入必要設定。

iOS 版不建立 `tap_assist_monthly` 訂閱，只保留廣告與移除廣告一次性內購。

## 收益結構

| 項目 | 形式 | 建議價格 | 商品 ID |
| --- | --- | ---: | --- |
| 免費版 | 顯示廣告 | 免費 | - |
| Pro 永久版 | 一次性內購，移除廣告 | NT$290 | `remove_ads_lifetime` |
| 點按輔助進階版 | 月訂閱 | NT$60/月 | `tap_assist_monthly` |

測試 APK 暫時開放點按輔助，避免尚未接入 Google Play Billing 時無法測試。正式上架版需依 Google Play Billing 的購買狀態解鎖。

## 廣告平台

建議使用 Google AdMob。

原因：

- Android 與 Google Play 生態整合最完整。
- 支援 Banner、Interstitial、Rewarded、Native 等常見格式。
- 官方文件與測試廣告 ID 完整，日後較容易除錯。
- 可先只用 Banner，避免工具 App 使用體驗被干擾。

## AdMob 註冊後需要建立

1. AdMob App
   - 平台：Android
   - 套件名稱：`com.ttt.liveassistant`
   - Play Console 合格公開金鑰 SHA-256：`66:83:A0:A9:EE:D1:57:26:11:FB:D1:6B:D2:11:F8:0F:6A:D7:89:08:F6:95:62:9C:E9:4F:B1:49:DC:AC:08:F5`
   - AdMob App ID：`ca-app-pub-2030886956293359~1113237891`

2. Banner 廣告單元
   - 建議名稱：`main_settings_banner`
   - 位置：主設定頁「來源語言」上方。
   - Banner Ad Unit ID：`ca-app-pub-2030886956293359/8794174954`
   - Debug 版使用 Google 測試 Banner ID：`ca-app-pub-3940256099942544/9214589741`

3. 啟動插頁廣告單元
   - 建議名稱：`daily_open_interstitial`
   - 位置：打開主 App 後顯示一次。
   - 正式 Interstitial Ad Unit ID：`ca-app-pub-2030886956293359/2658253850`
   - Debug 版使用 Google 測試 Interstitial ID：`ca-app-pub-3940256099942544/1033173712`

## 廣告放置規則

可以放：

- 主設定頁「來源語言」上方 Banner。
- 開啟 App 後顯示一次插頁。
- 設定頁自然切換時的低頻插頁。

不要放：

- 懸浮窗內。
- 使用者正在輸入文字時。
- 點按輔助執行中。
- 複製到剪貼簿的瞬間。
- 系統無障礙授權流程前後。

## Google Play Billing 商品

需要在 Play Console 建立：

1. 一次性商品
   - 商品 ID：`remove_ads_lifetime`
   - 名稱：`Pro 永久版 - 移除廣告`
   - 價格：NT$290
   - 權益：不顯示廣告。

2. 訂閱商品
   - 商品 ID：`tap_assist_monthly`
   - 名稱：`點按輔助進階版`
   - Base plan：月訂閱
   - 價格：NT$60/月
   - 權益：正式版啟用點按輔助。

## 上架合規提醒

- 透過 Google Play 發布時，移除廣告與訂閱功能都屬於 App 內數位功能，需使用 Google Play Billing。
- 無障礙點按功能必須在 App 內清楚揭露用途，並由使用者手動開啟、手動開始。
- 商店文案不要主打「遊戲連點」、「搶購」、「刷」、「自動操作第三方 App」等高風險用途。
- 建議文案使用「可見、限時、由使用者手動啟動的點按輔助，用於減輕重複性操作負擔」。

## 後續接入工作

拿到 AdMob 與 Play Console 商品資料後，再實作：

1. 加入 Google Mobile Ads SDK。
2. 在 Manifest 放入正式 AdMob App ID。
3. 建立 Banner AdView 並只在未購買 `remove_ads_lifetime` 時顯示。
4. 加入 Google Play Billing Library。
5. 查詢 `remove_ads_lifetime` 與 `tap_assist_monthly` 商品。
6. 完成購買、恢復購買與訂閱狀態判斷。
7. 正式版用訂閱狀態控制點按輔助；debug 版可保留測試開關。

目前 Android 專案接入的 Google Mobile Ads SDK：

```text
com.google.android.gms:play-services-ads:24.3.0
```

Debug 版會使用 Google 官方測試 Banner ID，Release 版才使用正式 Banner ID。

## app-ads.txt

本專案已準備 Firebase Hosting 靜態檔案：

```text
hosting/app-ads.txt
```

內容：

```text
google.com, pub-2030886956293359, DIRECT, f08c47fec0942fa0
```

Firebase Hosting 已部署，請在 Google Play Console 的開發人員網站填入：

```text
https://tap-translate-assistant.web.app
```

AdMob 會檢查：

```text
https://tap-translate-assistant.web.app/app-ads.txt
```
