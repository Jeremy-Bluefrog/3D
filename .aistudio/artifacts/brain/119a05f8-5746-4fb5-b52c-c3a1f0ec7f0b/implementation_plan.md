# Depth3D - 2D 照片轉 3D 空間景深應用程式 (純本地離線 ＋ Material You 動態色彩)

將普通 2D 相片全自動轉化為具備立體景深層次的 3D 空間相片。**100% 於手機裝置端本地運算，絕不使用網路 AI**。深度融合 **Material You 動態色彩（Dynamic Color）** 系統，並透過手機陀螺儀傾斜感應與手勢滑動，即時呈現多角度自然視差與光學景深（Bokeh）虛化效果。

### User Review & Critical Decisions

> [!IMPORTANT]
> 依據最新要求，系統全面採用**「Material You 動態色彩（Dynamic Color）」**，在 Android 12+（API 31+）上根據使用者桌布即時動態抽取配色，提供高度個人化且一致的系統級美學體驗。

- **已確認決策 1：嚴格禁止網路 AI，全面採用本地端推論**
  - 核心景深估算完全在 Android 本地裝置（On-Device）執行。結合卷積深度特徵分析、多尺度顯著性主體分割（Multi-scale Saliency）與雙邊引導平滑濾波（Edge-preserving Bilateral Filter），離線即可生成高精度景深地圖（Depth Map）。
- **已確認決策 2：Material You 動態色彩（Dynamic Color）**
  - 在 `Theme.kt` 中全面啟用 `dynamicDarkColorScheme` / `dynamicLightColorScheme`，完美融入 Android 系統風格，未達 Android 12 時則優雅回退至沉浸式電影藍灰光學主題。
- **已確認決策 3：體感互動與雙重視差視角**
  - 支援 Android 硬體陀螺儀/加速度旋轉向量感測器（`TYPE_ROTATION_VECTOR`）即時傾斜追蹤，並融合手指多維度觸控拖曳視角。同時具備一鍵自動視差巡航（3D Wiggle），在電腦模擬器或靜置時也能隨時欣賞立體景深。
- **已確認決策 4：高品質遮擋修補（Occlusion Inpainting）與光學散景（Bokeh）**
  - 實作「品質第一」的高精度視差演算法，針對傾斜角度下拉伸產生的邊緣空洞，實作雙層外推延伸（Extrapolation Fill），徹底消除黑邊與破面撕裂；搭配點擊切換焦點（Tap-to-Focus）與真實光學光圈虛化。

---

### 1. Overview & Core Concept

- **核心定位**：完全離線、無網路依賴、具備 Material You 動態色彩的 3D 空間相片工坊。保護照片隱私的同時，將 2D 平面記憶轉化為栩栩如生的全息立體相片。
- **目標用戶**：注重隱私安全、追求極致相片畫質與立體空間感的行動攝影愛好者。
- **核心價值**：
  - **零網路消耗、零隱私風險**：照片不出本機，離線隨開即用。
  - **動態色彩美學**：UI 元件配色隨手機桌布靈動變化。
  - **真實空間視差**：轉動手機即可透過物理透視觀察主體與背景間的深淺距離。

---

### 2. User Experience & Visual Design

#### 主要操作流程（User Flows）

1. **3D 空間藝廊（Gallery & Showcase）**：
   - 預載高品質 3D 範例（人像、寵物、微距、自然風光），首次開啟即可立即把玩真實 3D 效果。
   - 底部設有大型「匯入照片（Photo Picker）」與「相機拍攝」按鈕。
2. **純本機深度運算與立體化（On-Device Depth Processing）**：
   - 匯入相片後，以動態色彩幾何光波掃描動畫呈現「特徵卷積 ➔ 本地深度推論 ➔ 邊緣細節補強 ➔ 遮擋延伸」的高品質運算進程。
3. **沉浸式 3D 空間檢視器（3D Spatial Viewer）**：
   - **實體手機傾斜**：手機朝左、右、上、下微幅傾斜，前景主體與背景產生細膩的真實空間位移。
   - **手指觸控視角**：在螢幕上滑動可自由旋轉觀察視角，放手後平滑回正。
   - **點擊對焦（Tap to Focus）**：點擊主體或背景，對焦平面瞬間轉移，非對焦點呈現光學鏡頭散景。
   - **光學參數與視差控制**：
     - *景深強度（Parallax Depth Multiplier）*：柔和（Subtle）、自然（Natural）、強烈（Dramatic）。
     - *光圈散景（Aperture Bokeh）*：自由調整背景虛化程度。
     - *模式切換*：陀螺儀體感 / 觸控控制 / 自動 3D 搖擺預覽。
     - *景深地圖檢視（Depth Map Toggle）*：一鍵透視灰階深度圖，驗證空間層次。
4. **保存與歷史記錄（Local Save & Revisit）**：
   - 本地儲存深度資料與視差參數，日後於藝廊一鍵喚醒立體檢視。

#### 視覺風格與調性（Visual Identity & Theme）

- **設計風格**：Material 3 現代感結合電影級光學質感。
- **動態色彩（Dynamic Color）**：
  - Android 12+（API 31+）自動適配使用者桌布生成的 `dynamicDarkColorScheme` / `dynamicLightColorScheme`。
  - 容器與按鈕全面採用 M3 `Surface`, `PrimaryContainer`, `SecondaryContainer` 等語意色票。
  - 預設備援深色方案：冷杉藍黑 `#0D1117`、主調 `#38BDF8`、次調 `#818CF8`。

---

### 3. Key Product Decisions & Trade-Offs

#### 決策 1：純本地端深度推論架構 (No Cloud / Network AI)
- **選擇方案**：在 Android 裝置本地端實作多尺度深度特徵抽取器（Multi-Scale Gradient + Visual Saliency + Edge Guided Depth Estimation），並提供高精度像素層次分配。
- **原因**：完全滿足使用者「絕不使用網路 AI」的硬性要求，100% 離線運作，毫無網路延遲且保護敏感個人隱私。

#### 決策 2：雙邊引導濾波與邊緣無縫修補 (Occlusion Inpainting)
- **選擇方案**：深度圖邊緣對齊原圖邊界，並對傾斜產生的遮蔽邊緣進行像素外推（Background Dilation & Extrapolation）。
- **原因**：視差位移時若直接偏移，前景邊界後方會出現黑洞或斷層；透過外推填充背景，達到「品質第一」的無破綻視覺饗宴。

#### 決策 3：感測器低通濾波與多維度輸入平滑化
- **選擇方案**：陀螺儀與重力向量結合一階低通濾波器（IIR Low-Pass Filter），並與觸控手勢進行平滑融合。
- **原因**：消弭人手自然微顫產生的畫面躁動，保證 60fps 絲滑電影級運鏡手感。

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Depth3D App UI Layer                 │
│         (Powered by M3 Dynamic Color Theming)          │
│  ┌───────────────────────┐  ┌───────────────────────┐  │
│  │    3D Gallery View    │  │  3D Spatial Viewer    │  │
│  │ (Presets + Local 3D)  │  │ (Gyroscope + Parallax)│  │
│  └───────────┬───────────┘  └───────────┬───────────┘  │
└──────────────┼──────────────────────────┼──────────────┘
               ▼                          ▼
┌────────────────────────────────────────────────────────┐
│                   ViewModel & State                    │
│  - Active Photo & Depth Map State                      │
│  - Gyroscope / Tilt Coordinates (X, Y)                 │
│  - Tap-to-Focus Plane (0.0~1.0) & Bokeh Radius         │
└──────────────┬──────────────────────────┬──────────────┘
               ▼                          ▼
┌────────────────────────────────────────────────────────┐
│         100% On-Device Local Processing Engine         │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Local Depth Engine (Purely Offline)              │  │
│  │ - Saliency Subject Segmentation                  │  │
│  │ - Gradient Depth Inference & Bilateral Filter    │  │
│  │ - Foreground / Midground / Background Planes     │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Parallax Renderer (GPU / Canvas 60fps)           │  │
│  │ - Pixel Offset by (Tilt * DepthDelta)            │  │
│  │ - Occlusion Hole Extrapolation Inpainting        │  │
│  │ - Lens Blur & Aperture Bokeh Shader              │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │ Sensor Fusion: Gyroscope + Accelerometer Filter  │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```
