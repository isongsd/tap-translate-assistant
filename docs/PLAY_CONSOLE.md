# Google Play Console 設定

## 應用程式基本資料

- 應用程式名稱：`點按翻譯助手`
- 套件名稱：`com.ttt.liveassistant`
- 預設語言：`繁體中文 - zh-TW`
- 類型：`應用程式`
- 是否收費：`免費`

本專案採免費下載，並透過廣告、移除廣告內購與點按輔助訂閱收益。

## 合格的公開金鑰

Google Play Console 顯示既有套件名稱 `com.ttt.liveassistant`，建立應用程式時需選擇合格公開金鑰的 SHA-256 憑證指紋。

目前記錄的合格公開金鑰 SHA-256：

```text
66:83:A0:A9:EE:D1:57:26:11:FB:D1:6B:D2:11:F8:0F:6A:D7:89:08:F6:95:62:9C:E9:4F:B1:49:DC:AC:08:F5
```

## 後續設定項目

- 商品詳細資料：填寫 App 名稱、簡短說明、完整說明、截圖與圖示。
- 應用程式內容：完成廣告、資料安全性、目標客群、權限與無障礙用途聲明。
- 內購商品：建立 `remove_ads_lifetime`。
- 訂閱商品：建立 `tap_assist_monthly`。
- AdMob：使用同一套件名稱建立 Android App。

## 商店圖示

B 版圖示包已保存於：

```text
D:\projects\TTt\store-assets\icons\app-icon-b
```

Google Play 商品詳細資料的 512x512 圖示請使用：

```text
D:\projects\TTt\store-assets\icons\app-icon-b\icon-512.png
```

App Store 或高解析備用圖示可使用：

```text
D:\projects\TTt\store-assets\icons\app-icon-b\AppIcon-1024.png
```

Android App 內 launcher icon 已使用同一包的 density mipmap 圖示。

## 認領套件名稱用 APK

目前 Play Console 要求先上傳 APK 證明套件名稱擁有權。因為合格公開金鑰是這台電腦的 Android debug keystore，請勿上傳一般 `app-debug.apk`，而是使用認領用 release APK：

1. 在 Play Console 的「簽署並上傳 APK」流程中，複製 Google 顯示的 snippet / 權杖文字。
2. 貼到下列檔案，覆蓋 placeholder：

```text
D:\projects\TTt\app\src\main\assets\adi-registration.properties
```

檔名必須完全是：

```text
adi-registration.properties
```

3. 重新打包認領 APK：

```powershell
.\scripts\build-play-claim-apk.ps1
```

產物：

```text
D:\projects\TTt\app\build\outputs\apk\release\app-play-claim.apk
```

這個 APK 用於 Play Console 套件名稱認領流程，簽章 SHA-256 會對應上方記錄的合格公開金鑰。正式上架版本仍建議使用正式 release signing / AAB 流程，並妥善備份上傳金鑰。

若 Play Console 顯示「上傳的 APK 沒有所需權杖檔案」，代表 APK 內沒有包含 `assets/adi-registration.properties`，或檔案內容不是 Play Console 提供的 snippet。

## 封閉測試用 AAB

若封閉測試頁面不接受 APK，請改用 Android App Bundle：

第一次產生封閉測試 AAB 前，先建立正式 release keystore：

```powershell
.\scripts\create-release-keystore.ps1
```

這會產生：

```text
D:\projects\TTt\keystore\tap-translate-release.jks
D:\projects\TTt\keystore\release-signing.properties
```

務必備份這兩個檔案。封閉測試與後續上架版本不能使用 Android debug keystore，否則 Play Console 會顯示「在偵錯模式下完成簽署」。

目前 release keystore 憑證：

```text
Owner: CN=Lurela App Lab, O=Lurela App Lab, C=TW
SHA-256: C1:4C:07:6C:99:BA:A4:E7:26:39:AB:54:46:EE:79:42:52:9B:36:77:7B:8A:FC:42:BB:B3:28:63:3F:2F:42:51
```

接著產生封閉測試 AAB：

```powershell
.\scripts\build-play-release-aab.ps1
```

產物：

```text
D:\projects\TTt\app\build\outputs\bundle\release\app-release.aab
```

上傳封閉測試版本時，選擇 `app-release.aab`。
