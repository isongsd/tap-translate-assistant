# Firebase Hosting

本專案使用 Firebase Hosting 放置公開網站、隱私權政策與 `app-ads.txt`。

## 需要部署的檔案

- `hosting/index.html`：簡單首頁。
- `hosting/privacy.html`：隱私權政策。
- `hosting/app-ads.txt`：AdMob app-ads.txt。

`app-ads.txt` 目前內容：

```text
google.com, pub-2030886956293359, DIRECT, f08c47fec0942fa0
```

## 建立 Firebase 專案

1. 到 Firebase Console 建立專案。
2. 專案方案選 Spark 免費方案即可。
3. 開啟 Hosting。
4. 在本機登入 Firebase CLI：

```powershell
firebase login
```

5. 指定專案：

```powershell
firebase use --add
```

6. 部署：

```powershell
firebase deploy --only hosting
```

## 部署後要填到後台

Firebase Hosting 網址是：

```text
https://tap-translate-assistant.web.app
```

Google Play Console：

- 隱私權政策網址：`https://tap-translate-assistant.web.app/privacy.html`
- 開發人員網站：`https://tap-translate-assistant.web.app`

AdMob 會檢查：

```text
https://tap-translate-assistant.web.app/app-ads.txt
```

AdMob 抓取與驗證可能需要數小時到 7 天。
