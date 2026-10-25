package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.example.engine.LocalDepthEstimator
import com.example.model.DepthPhoto

/**
 * 預設高品質 3D 空間照片資料提供者
 *
 * 預置 4 幅具備強烈空間深淺層次的攝影級場景，供使用者於首次啟動時立即把玩立體視差。
 */
object SamplePhotosProvider {

    fun getSamplePhotos(): List<DepthPhoto> {
        val photos = mutableListOf<DepthPhoto>()

        // 1. 暖陽慵懶橘貓 (Cat Portrait)
        val catBmp = createCatPortraitBitmap()
        val catDepth = LocalDepthEstimator.estimateDepth(catBmp)
        photos.add(
            DepthPhoto(
                id = "preset_cat",
                title = "暖陽慵懶橘貓",
                subtitle = "超特寫人像景深 · 絨毛輪廓立體分離",
                originalBitmap = catBmp,
                depthBitmap = catDepth.depthMap,
                focusPlane = 0.75f,
                parallaxMultiplier = 1.2f,
                bokehRadius = 10f,
                isPreset = true
            )
        )

        // 2. 賽博龐克透視走廊 (Cyberpunk Corridor)
        val corridorBmp = createCorridorBitmap()
        val corridorDepth = LocalDepthEstimator.estimateDepth(corridorBmp)
        photos.add(
            DepthPhoto(
                id = "preset_corridor",
                title = "賽博光影縱深走廊",
                subtitle = "極限透視導引線 · 霓虹光暈空間層次",
                originalBitmap = corridorBmp,
                depthBitmap = corridorDepth.depthMap,
                focusPlane = 0.50f,
                parallaxMultiplier = 1.5f,
                bokehRadius = 8f,
                isPreset = true
            )
        )

        // 3. 林間微距晨露花卉 (Forest Macro Flower)
        val flowerBmp = createMacroFlowerBitmap()
        val flowerDepth = LocalDepthEstimator.estimateDepth(flowerBmp)
        photos.add(
            DepthPhoto(
                id = "preset_flower",
                title = "林間晨露微距花卉",
                subtitle = "花瓣水珠銳利對焦 · 背景柔美大光圈散景",
                originalBitmap = flowerBmp,
                depthBitmap = flowerDepth.depthMap,
                focusPlane = 0.85f,
                parallaxMultiplier = 1.3f,
                bokehRadius = 14f,
                isPreset = true
            )
        )

        // 4. 暮色雪山倒影湖泊 (Alpine Lake Landscape)
        val lakeBmp = createAlpineLakeBitmap()
        val lakeDepth = LocalDepthEstimator.estimateDepth(lakeBmp)
        photos.add(
            DepthPhoto(
                id = "preset_lake",
                title = "暮色雪山倒影湖泊",
                subtitle = "群峰連綿層疊 · 水面近景倒影廣角景深",
                originalBitmap = lakeBmp,
                depthBitmap = lakeDepth.depthMap,
                focusPlane = 0.40f,
                parallaxMultiplier = 1.1f,
                bokehRadius = 6f,
                isPreset = true
            )
        )

        return photos
    }

    private fun createCatPortraitBitmap(): Bitmap {
        val w = 720
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 溫馨客廳散景背景
        val bgShader = RadialGradient(
            w * 0.7f, h * 0.3f, w * 0.8f,
            intArrayOf(Color.parseColor("#4A2E18"), Color.parseColor("#1B120C"), Color.parseColor("#0F0A07")),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // 背景散景光斑
        paint.shader = null
        val bokehColors = intArrayOf(
            Color.argb(70, 255, 200, 120),
            Color.argb(50, 255, 170, 80),
            Color.argb(40, 200, 220, 255)
        )
        paint.color = bokehColors[0]
        canvas.drawCircle(w * 0.2f, h * 0.22f, 65f, paint)
        paint.color = bokehColors[1]
        canvas.drawCircle(w * 0.82f, h * 0.18f, 90f, paint)
        paint.color = bokehColors[2]
        canvas.drawCircle(w * 0.35f, h * 0.12f, 45f, paint)
        canvas.drawCircle(w * 0.88f, h * 0.38f, 55f, paint)

        // 前景木質桌檯
        paint.color = Color.parseColor("#2E1F14")
        canvas.drawRect(0f, h * 0.78f, w.toFloat(), h.toFloat(), paint)
        paint.color = Color.parseColor("#3D291C")
        canvas.drawRect(0f, h * 0.78f, w.toFloat(), h * 0.80f, paint)

        // 橘貓主體身體 (前景主體)
        paint.color = Color.parseColor("#D47328")
        canvas.drawOval(w * 0.2f, h * 0.50f, w * 0.8f, h * 0.90f, paint)

        // 貓咪頭部
        paint.color = Color.parseColor("#E68336")
        canvas.drawCircle(w * 0.5f, h * 0.44f, 160f, paint)

        // 貓耳朵
        val leftEar = Path().apply {
            moveTo(w * 0.36f, h * 0.34f)
            lineTo(w * 0.28f, h * 0.18f)
            lineTo(w * 0.44f, h * 0.26f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(w * 0.64f, h * 0.34f)
            lineTo(w * 0.72f, h * 0.18f)
            lineTo(w * 0.56f, h * 0.26f)
            close()
        }
        paint.color = Color.parseColor("#D47328")
        canvas.drawPath(leftEar, paint)
        canvas.drawPath(rightEar, paint)
        paint.color = Color.parseColor("#F4A698")
        val innerLeftEar = Path().apply {
            moveTo(w * 0.36f, h * 0.32f)
            lineTo(w * 0.30f, h * 0.21f)
            lineTo(w * 0.42f, h * 0.27f)
            close()
        }
        val innerRightEar = Path().apply {
            moveTo(w * 0.64f, h * 0.32f)
            lineTo(w * 0.70f, h * 0.21f)
            lineTo(w * 0.58f, h * 0.27f)
            close()
        }
        canvas.drawPath(innerLeftEar, paint)
        canvas.drawPath(innerRightEar, paint)

        // 貓咪臉部白斑與口鼻
        paint.color = Color.parseColor("#FFF4E8")
        canvas.drawOval(w * 0.38f, h * 0.45f, w * 0.62f, h * 0.56f, paint)

        // 碧綠清澈的大眼睛
        paint.color = Color.parseColor("#5DBB63")
        canvas.drawCircle(w * 0.42f, h * 0.41f, 32f, paint)
        canvas.drawCircle(w * 0.58f, h * 0.41f, 32f, paint)

        // 瞳孔
        paint.color = Color.parseColor("#152417")
        canvas.drawOval(w * 0.405f, h * 0.38f, w * 0.435f, h * 0.44f, paint)
        canvas.drawOval(w * 0.565f, h * 0.38f, w * 0.595f, h * 0.44f, paint)

        // 眼睛高光 (讓眼睛靈動立體)
        paint.color = Color.WHITE
        canvas.drawCircle(w * 0.415f, h * 0.395f, 9f, paint)
        canvas.drawCircle(w * 0.575f, h * 0.395f, 9f, paint)

        // 粉嫩小鼻子
        paint.color = Color.parseColor("#F28B82")
        val nose = Path().apply {
            moveTo(w * 0.485f, h * 0.48f)
            lineTo(w * 0.515f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.505f)
            close()
        }
        canvas.drawPath(nose, paint)

        // 鬍鬚
        paint.color = Color.parseColor("#EAEAEA")
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.38f, h * 0.50f, w * 0.18f, h * 0.47f, paint)
        canvas.drawLine(w * 0.38f, h * 0.51f, w * 0.17f, h * 0.52f, paint)
        canvas.drawLine(w * 0.38f, h * 0.52f, w * 0.20f, h * 0.57f, paint)
        canvas.drawLine(w * 0.62f, h * 0.50f, w * 0.82f, h * 0.47f, paint)
        canvas.drawLine(w * 0.62f, h * 0.51f, w * 0.83f, h * 0.52f, paint)
        canvas.drawLine(w * 0.62f, h * 0.52f, w * 0.80f, h * 0.57f, paint)
        paint.style = Paint.Style.FILL

        return bmp
    }

    private fun createCorridorBitmap(): Bitmap {
        val w = 720
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 深邃黑夜基底
        canvas.drawColor(Color.parseColor("#080A10"))

        val vpX = w * 0.5f
        val vpY = h * 0.48f

        // 天花板透視梯形
        val ceilShader = LinearGradient(
            vpX, vpY, vpX, 0f,
            Color.parseColor("#121828"), Color.parseColor("#090C14"),
            Shader.TileMode.CLAMP
        )
        paint.shader = ceilShader
        val ceilPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(w.toFloat(), 0f)
            lineTo(vpX + 40f, vpY - 30f)
            lineTo(vpX - 40f, vpY - 30f)
            close()
        }
        canvas.drawPath(ceilPath, paint)

        // 地面透視梯形
        val floorShader = LinearGradient(
            vpX, vpY, vpX, h.toFloat(),
            Color.parseColor("#151D30"), Color.parseColor("#0A0D15"),
            Shader.TileMode.CLAMP
        )
        paint.shader = floorShader
        val floorPath = Path().apply {
            moveTo(0f, h.toFloat())
            lineTo(w.toFloat(), h.toFloat())
            lineTo(vpX + 40f, vpY + 30f)
            lineTo(vpX - 40f, vpY + 30f)
            close()
        }
        canvas.drawPath(floorPath, paint)

        // 左右兩側牆壁霓虹光條
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f

        // 左側青藍光條 (Cyan)
        paint.color = Color.parseColor("#00E5FF")
        canvas.drawLine(0f, h * 0.2f, vpX - 40f, vpY - 15f, paint)
        canvas.drawLine(0f, h * 0.8f, vpX - 40f, vpY + 15f, paint)

        // 右側紫紅霓虹 (Magenta)
        paint.color = Color.parseColor("#FF007F")
        canvas.drawLine(w.toFloat(), h * 0.2f, vpX + 40f, vpY - 15f, paint)
        canvas.drawLine(w.toFloat(), h * 0.8f, vpX + 40f, vpY + 15f, paint)

        // 走廊中景門框 (Midground Portals)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        paint.color = Color.parseColor("#00E5FF")
        canvas.drawRect(w * 0.18f, h * 0.22f, w * 0.82f, h * 0.78f, paint)

        paint.color = Color.parseColor("#FF007F")
        canvas.drawRect(w * 0.32f, h * 0.34f, w * 0.68f, h * 0.66f, paint)

        // 走廊盡頭神秘光點 (Far Depth Light)
        paint.style = Paint.Style.FILL
        val endGlow = RadialGradient(
            vpX, vpY, 80f,
            intArrayOf(Color.WHITE, Color.parseColor("#80D8FF"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = endGlow
        canvas.drawCircle(vpX, vpY, 70f, paint)

        return bmp
    }

    private fun createMacroFlowerBitmap(): Bitmap {
        val w = 720
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 翠綠森林背景
        val bgShader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            Color.parseColor("#0D2818"), Color.parseColor("#04140A"),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // 森林散景柔光
        paint.shader = null
        paint.color = Color.argb(45, 120, 220, 140)
        canvas.drawCircle(w * 0.25f, h * 0.2f, 100f, paint)
        canvas.drawCircle(w * 0.75f, h * 0.15f, 140f, paint)
        canvas.drawCircle(w * 0.85f, h * 0.5f, 80f, paint)

        // 花莖 (中景)
        paint.color = Color.parseColor("#2D6A4F")
        paint.strokeWidth = 24f
        paint.style = Paint.Style.STROKE
        val stem = Path().apply {
            moveTo(w * 0.48f, h * 0.56f)
            quadTo(w * 0.42f, h * 0.75f, w * 0.35f, h.toFloat())
        }
        canvas.drawPath(stem, paint)

        // 前景大花瓣 (聚焦主體)
        paint.style = Paint.Style.FILL
        val flowerCenterX = w * 0.5f
        val flowerCenterY = h * 0.46f

        val petalCount = 8
        for (i in 0 until petalCount) {
            val angle = i * (360.0 / petalCount)
            canvas.save()
            canvas.rotate(angle.toFloat(), flowerCenterX, flowerCenterY)

            val petalShader = RadialGradient(
                flowerCenterX, flowerCenterY - 110f, 120f,
                Color.parseColor("#FF6584"), Color.parseColor("#C71585"),
                Shader.TileMode.CLAMP
            )
            paint.shader = petalShader
            canvas.drawOval(
                flowerCenterX - 45f, flowerCenterY - 170f,
                flowerCenterX + 45f, flowerCenterY - 20f,
                paint
            )
            canvas.restore()
        }

        // 花蕊金色核心
        val centerShader = RadialGradient(
            flowerCenterX, flowerCenterY, 55f,
            intArrayOf(Color.parseColor("#FFF176"), Color.parseColor("#F57F17")),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = centerShader
        canvas.drawCircle(flowerCenterX, flowerCenterY, 50f, paint)

        // 露珠 (極前景高光亮點)
        paint.shader = null
        paint.color = Color.argb(190, 255, 255, 255)
        canvas.drawCircle(flowerCenterX - 32f, flowerCenterY - 70f, 14f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(flowerCenterX - 35f, flowerCenterY - 73f, 4f, paint)

        return bmp
    }

    private fun createAlpineLakeBitmap(): Bitmap {
        val w = 720
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 暮色天空漸層
        val skyShader = LinearGradient(
            0f, 0f, 0f, h * 0.5f,
            intArrayOf(Color.parseColor("#2B1055"), Color.parseColor("#753A88"), Color.parseColor("#E06A55"), Color.parseColor("#FFBA7A")),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = skyShader
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, paint)

        // 暮色金霞夕陽
        paint.shader = null
        paint.color = Color.parseColor("#FFF3B0")
        canvas.drawCircle(w * 0.5f, h * 0.42f, 42f, paint)

        // 遠處雪山 (遠景)
        paint.color = Color.parseColor("#442858")
        val distantMountain = Path().apply {
            moveTo(0f, h * 0.5f)
            lineTo(w * 0.28f, h * 0.28f)
            lineTo(w * 0.52f, h * 0.44f)
            lineTo(w * 0.78f, h * 0.24f)
            lineTo(w.toFloat(), h * 0.5f)
            close()
        }
        canvas.drawPath(distantMountain, paint)

        // 雪峰金光頂
        paint.color = Color.parseColor("#FFE0B2")
        val snowPeak1 = Path().apply {
            moveTo(w * 0.28f, h * 0.28f)
            lineTo(w * 0.24f, h * 0.34f)
            lineTo(w * 0.32f, h * 0.35f)
            close()
        }
        val snowPeak2 = Path().apply {
            moveTo(w * 0.78f, h * 0.24f)
            lineTo(w * 0.73f, h * 0.31f)
            lineTo(w * 0.83f, h * 0.32f)
            close()
        }
        canvas.drawPath(snowPeak1, paint)
        canvas.drawPath(snowPeak2, paint)

        // 中景針葉林湖岸
        paint.color = Color.parseColor("#1B1A30")
        val shore = Path().apply {
            moveTo(0f, h * 0.56f)
            lineTo(w * 0.4f, h * 0.51f)
            lineTo(w.toFloat(), h * 0.54f)
            lineTo(w.toFloat(), h * 0.58f)
            lineTo(0f, h * 0.60f)
            close()
        }
        canvas.drawPath(shore, paint)

        // 前景鏡面平湖
        val lakeShader = LinearGradient(
            0f, h * 0.56f, 0f, h.toFloat(),
            Color.parseColor("#28163E"), Color.parseColor("#0F0C1E"),
            Shader.TileMode.CLAMP
        )
        paint.shader = lakeShader
        canvas.drawRect(0f, h * 0.56f, w.toFloat(), h.toFloat(), paint)

        // 湖面近景倒影
        paint.shader = null
        paint.color = Color.argb(60, 255, 186, 122)
        canvas.drawOval(w * 0.35f, h * 0.62f, w * 0.65f, h * 0.68f, paint)

        // 近景湖邊蘆葦礁石 (極前景)
        paint.color = Color.parseColor("#0C0A14")
        canvas.drawOval(-40f, h * 0.88f, w * 0.35f, h * 1.08f, paint)
        canvas.drawOval(w * 0.75f, h * 0.92f, w * 1.15f, h * 1.10f, paint)

        return bmp
    }
}
