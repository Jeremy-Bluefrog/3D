# Depth3D - 2D 照片轉 3D 空間景深 Android 應用程式

將普通 2D 相片全自動轉化為具備立體景深層次的 3D 空間相片。**100% 於手機裝置端本地運算，絕不傳輸至網路或使用雲端 AI**。支援 Material You 動態色彩、手機陀螺儀體感傾斜視差與光學散景（Bokeh）。

---

## 🚀 如何使用 GitHub Actions 自動建立與下載 APK

本專案已內建完整的 GitHub Actions 自動化建置工作流程（位於 `.github/workflows/build-apk.yml`），無需在本地電腦安裝複雜的 Android Studio 即可在雲端完成 APK 編譯：

### 步驟 1：將專案推送到 GitHub
- 在 Google AI Studio 介面右上角選單中，選擇 **Push to GitHub**（或下載 ZIP 專案解壓縮後推送到您的 GitHub 儲存庫）。

### 步驟 2：觸發 GitHub Actions 自動編譯
1. 開啟您的 GitHub 專案儲存庫頁面。
2. 點擊頂部導航列的 **Actions** 分頁。
3. 在左側工作流程清單中點選 **Build Android APK**。
4. 點擊右側的 **Run workflow** 按鈕 ➔ 選擇 `main` 分支 ➔ 點擊綠色 **Run workflow** 開始建置。（每次有新的 code commit 也會自動觸發）

### 步驟 3：下載生成的 APK 檔案
1. 等待工作流程執行完成（顯示綠色勾勾 ✅）。
2. 點擊進入該次建置記錄。
3. 滑動到頁面底部的 **Artifacts** 區塊，點擊 **Depth3D-debug-apk** 即可下載 ZIP 壓縮包。
4. 解壓縮後取得 `app-debug.apk`，傳輸到 Android 手機即可直接安裝體驗！

---

## 🛠 本地端手動編譯

若您希望在本地電腦進行編譯：

```bash
# 賦予 Gradle 執行權限
chmod +x gradlew

# 編譯 Debug APK
./gradlew assembleDebug

# 編譯完成的 APK 位於：
# app/build/outputs/apk/debug/app-debug.apk
```
