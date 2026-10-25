package com.example.model

import android.graphics.Bitmap

/**
 * 代表一張具備 3D 空間景深層次的相片實體
 */
data class DepthPhoto(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val originalBitmap: Bitmap,
    val depthBitmap: Bitmap,
    val focusPlane: Float = 0.65f, // 0.0 (遠景) ~ 1.0 (近景)
    val parallaxMultiplier: Float = 1.0f, // 視差強度倍率
    val bokehRadius: Float = 8.0f, // 光圈散景強度
    val isPreset: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
) {
    val aspectRatio: Float
        get() = if (originalBitmap.height > 0) {
            originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        } else {
            1.0f
        }
}
