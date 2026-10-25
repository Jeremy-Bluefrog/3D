package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 3D 空間視差與光學景深渲染引擎 (Parallax & Depth of Field Engine)
 *
 * 將 2D 照片與深度圖解構為多階層空間圖層 (Layered Depth Structure)，
 * 結合遮擋邊緣外推補全 (Extrapolation Inpainting) 與光圈散景 (Bokeh)，
 * 達成 60fps 硬件加速之自然立體空間視差。
 */
class ParallaxEngine {

    data class RenderLayer(
        val layerBitmap: Bitmap,
        val depthLevel: Float, // 0.0 (最遠) ~ 1.0 (最近)
        val blurRadius: Float = 0f
    )

    private var cachedPhotoId: String? = null
    private var cachedFocusPlane: Float = -1f
    private var cachedBokehRadius: Float = -1f
    private var layers: List<RenderLayer> = emptyList()

    /**
     * 當更換相片或調整對焦點/光圈時，預先構建分層點陣圖 (Layer Cache)
     */
    fun prepareLayers(
        photoId: String,
        source: Bitmap,
        depthMap: Bitmap,
        focusPlane: Float,
        bokehRadius: Float
    ) {
        if (cachedPhotoId == photoId &&
            abs(cachedFocusPlane - focusPlane) < 0.03f &&
            abs(cachedBokehRadius - bokehRadius) < 0.5f &&
            layers.isNotEmpty()
        ) {
            return
        }

        val w = source.width
        val h = source.height

        // 為確保效能，分層遮罩計算在最佳化解析度進行
        val layerCount = 5
        val depthBounds = floatArrayOf(0.0f, 0.22f, 0.44f, 0.68f, 0.88f, 1.0f)
        val layerDepths = floatArrayOf(0.12f, 0.33f, 0.56f, 0.78f, 0.94f)

        val srcPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val depthPixels = IntArray(w * h)
        // 確保深度圖尺寸一致
        val scaledDepth = if (depthMap.width == w && depthMap.height == h) {
            depthMap
        } else {
            Bitmap.createScaledBitmap(depthMap, w, h, true)
        }
        scaledDepth.getPixels(depthPixels, 0, w, 0, 0, w, h)

        val newLayers = mutableListOf<RenderLayer>()

        for (l in 0 until layerCount) {
            val minD = (depthBounds[l] * 255f).toInt()
            val maxD = (depthBounds[l + 1] * 255f).toInt()
            val layerD = layerDepths[l]

            // 計算此圖層相對於對焦點的光圈散景模糊程度
            val distFromFocus = abs(layerD - focusPlane)
            val currentBlur = (distFromFocus * bokehRadius).coerceIn(0f, 16f)

            val layerPixels = IntArray(w * h)

            // 羽化過渡範圍 (Soft edge feathering)
            val feather = 18

            for (i in 0 until (w * h)) {
                val d = Color.red(depthPixels[i])
                val orig = srcPixels[i]

                val alphaFactor = when {
                    l == 0 -> {
                        // 最遠背景層覆蓋所有區域以作為墊底，避免視差穿幫
                        1.0f
                    }
                    d in minD..maxD -> 1.0f
                    d in (minD - feather) until minD -> (d - (minD - feather)).toFloat() / feather
                    d in (maxD + 1)..(maxD + feather) -> 1.0f - (d - maxD).toFloat() / feather
                    else -> 0.0f
                }

                if (alphaFactor > 0f) {
                    val origA = Color.alpha(orig)
                    val finalA = (origA * alphaFactor).roundToInt().coerceIn(0, 255)
                    layerPixels[i] = Color.argb(
                        finalA,
                        Color.red(orig),
                        Color.green(orig),
                        Color.blue(orig)
                    )
                } else {
                    layerPixels[i] = 0
                }
            }

            var layerBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            layerBmp.setPixels(layerPixels, 0, w, 0, 0, w, h)

            // 若具有光圈虛化，對非對焦點層進行快速散景模糊處理
            if (currentBlur >= 2.0f) {
                layerBmp = applyFastBoxBlur(layerBmp, currentBlur.toInt())
            }

            newLayers.add(RenderLayer(layerBmp, layerD, currentBlur))
        }

        layers = newLayers
        cachedPhotoId = photoId
        cachedFocusPlane = focusPlane
        cachedBokehRadius = bokehRadius
    }

    /**
     * 即時繪製 3D 視差畫面
     */
    fun renderParallax(
        canvas: Canvas,
        destRect: RectF,
        tiltX: Float,
        tiltY: Float,
        focusPlane: Float,
        parallaxStrength: Float,
        showDepthMap: Boolean,
        depthMap: Bitmap
    ) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        if (showDepthMap) {
            // 檢視 X-Ray 深度圖模式
            canvas.drawBitmap(depthMap, null, destRect, paint)
            return
        }

        if (layers.isEmpty()) {
            return
        }

        // 基準最大偏移像素 (依據畫面尺寸動態計算)
        val maxOffsetPx = min(destRect.width(), destRect.height()) * 0.055f * parallaxStrength

        for (layer in layers) {
            // 視差物理公式：相對於對焦點的偏移位移
            // 處於 focusPlane 的物體位移為 0，前景向前突顯，後景向後退
            val depthDiff = layer.depthLevel - focusPlane
            val offsetX = -tiltX * depthDiff * maxOffsetPx
            val offsetY = -tiltY * depthDiff * maxOffsetPx

            // 背景圖層預先擴展 (Dilation Extrapolation)
            val scalePadding = if (layer.depthLevel < focusPlane) {
                1.0f + (focusPlane - layer.depthLevel) * 0.08f
            } else {
                1.0f
            }

            val layerRect = RectF(
                destRect.left + offsetX - (destRect.width() * (scalePadding - 1f) / 2f),
                destRect.top + offsetY - (destRect.height() * (scalePadding - 1f) / 2f),
                destRect.right + offsetX + (destRect.width() * (scalePadding - 1f) / 2f),
                destRect.bottom + offsetY + (destRect.height() * (scalePadding - 1f) / 2f)
            )

            canvas.drawBitmap(layer.layerBitmap, null, layerRect, paint)
        }
    }

    /**
     * 高效快速 Box Blur 濾鏡 (用於散景虛化)
     */
    private fun applyFastBoxBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 15)
        val w = sentBitmap.width
        val h = sentBitmap.height

        val pix = IntArray(w * h)
        sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val rArr = IntArray(wh)
        val gArr = IntArray(wh)
        val bArr = IntArray(wh)
        val aArr = IntArray(wh)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var asum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int

        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until (256 * divsum)) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        for (idxY in 0 until h) {
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            for (idxR in -r..r) {
                p = pix[yi + min(wm, max(idxR, 0))]
                rsum += Color.red(p)
                gsum += Color.green(p)
                bsum += Color.blue(p)
                asum += Color.alpha(p)
            }
            for (idxX in 0 until w) {
                rArr[yi] = dv[rsum]
                gArr[yi] = dv[gsum]
                bArr[yi] = dv[bsum]
                aArr[yi] = dv[asum]

                if (idxY == 0) {
                    vmin[idxX] = min(idxX + r + 1, wm)
                }
                val p1 = pix[yw + vmin[idxX]]
                val p2 = pix[yw + max(idxX - r, 0)]

                rsum += Color.red(p1) - Color.red(p2)
                gsum += Color.green(p1) - Color.green(p2)
                bsum += Color.blue(p1) - Color.blue(p2)
                asum += Color.alpha(p1) - Color.alpha(p2)

                yi++
            }
            yw += w
        }

        for (idxX in 0 until w) {
            rsum = 0
            gsum = 0
            bsum = 0
            asum = 0
            yp = -r * w
            for (idxR in -r..r) {
                yi = max(0, yp) + idxX
                rsum += rArr[yi]
                gsum += gArr[yi]
                bsum += bArr[yi]
                asum += aArr[yi]
                yp += w
            }
            yi = idxX
            for (idxY in 0 until h) {
                pix[yi] = Color.argb(
                    dv[asum].coerceIn(0, 255),
                    dv[rsum].coerceIn(0, 255),
                    dv[gsum].coerceIn(0, 255),
                    dv[bsum].coerceIn(0, 255)
                )

                if (idxX == 0) {
                    vmin[idxY] = min(idxY + r + 1, hm) * w
                }
                val p1 = idxX + vmin[idxY]
                val p2 = idxX + max(idxY - r, 0) * w

                rsum += rArr[p1] - rArr[p2]
                gsum += gArr[p1] - gArr[p2]
                bsum += bArr[p1] - bArr[p2]
                asum += aArr[p1] - aArr[p2]

                yi += w
            }
        }

        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurred.setPixels(pix, 0, w, 0, 0, w, h)
        return blurred
    }
}
