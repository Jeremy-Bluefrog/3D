package com.example.ui.components

import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DepthPhoto
import com.example.viewmodel.Depth3DViewModel
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * 3D 空間視差畫布元件 (Interactive 3D Parallax Canvas)
 */
@Composable
fun ParallaxView(
    viewModel: Depth3DViewModel,
    photo: DepthPhoto,
    modifier: Modifier = Modifier
) {
    val motionState by viewModel.motionController.motionState.collectAsState()
    val focusPlane by viewModel.focusPlane.collectAsState()
    val parallaxStrength by viewModel.parallaxStrength.collectAsState()
    val showDepthMapOnly by viewModel.showDepthMapOnly.collectAsState()
    val tapFocusEvent by viewModel.tapFocusEvent.collectAsState()

    // 對焦圈動畫
    val focusAnimRadius = remember { Animatable(0f) }
    val focusAnimAlpha = remember { Animatable(0f) }

    LaunchedEffect(tapFocusEvent) {
        tapFocusEvent?.let {
            launch {
                focusAnimRadius.snapTo(12f)
                focusAnimAlpha.snapTo(1.0f)
                focusAnimRadius.animateTo(45f, tween(350))
                focusAnimAlpha.animateTo(0.0f, tween(200))
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("parallax_view_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()

        val photoAspect = photo.aspectRatio
        val (renderW, renderH) = if (containerWidth / containerHeight > photoAspect) {
            val h = containerHeight
            val w = h * photoAspect
            Pair(w, h)
        } else {
            val w = containerWidth
            val h = w / photoAspect
            Pair(w, h)
        }

        val density = LocalDensity.current
        val renderWDp = with(density) { renderW.toDp() }
        val renderHDp = with(density) { renderH.toDp() }

        Box(
            modifier = Modifier
                .size(width = renderWDp, height = renderHDp)
                .shadow(16.dp, shape = MaterialTheme.shapes.large)
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black)
                .pointerInput(photo.id) {
                    detectTapGestures { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        viewModel.tapToFocus(normX, normY)
                    }
                }
                .pointerInput(photo.id) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            viewModel.motionController.onTouchDrag(
                                deltaX = dragAmount.x,
                                deltaY = dragAmount.y,
                                viewWidth = size.width.toFloat(),
                                viewHeight = size.height.toFloat()
                            )
                        },
                        onDragEnd = {
                            viewModel.motionController.onTouchRelease()
                        },
                        onDragCancel = {
                            viewModel.motionController.onTouchRelease()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().testTag("parallax_canvas")) {
                val canvasW = size.width
                val canvasH = size.height
                val destRect = RectF(0f, 0f, canvasW, canvasH)

                drawIntoCanvas { composeCanvas ->
                    viewModel.parallaxEngine.renderParallax(
                        canvas = composeCanvas.nativeCanvas,
                        destRect = destRect,
                        tiltX = motionState.tiltX,
                        tiltY = motionState.tiltY,
                        focusPlane = focusPlane,
                        parallaxStrength = parallaxStrength,
                        showDepthMap = showDepthMapOnly,
                        depthMap = photo.depthBitmap
                    )
                }

                // 繪製點擊對焦光環 (Focus Indicator)
                tapFocusEvent?.let { event ->
                    if (focusAnimAlpha.value > 0.01f) {
                        val centerX = event.x * canvasW
                        val centerY = event.y * canvasH
                        val primaryColor = Color(0xFF64B5F6)

                        drawCircle(
                            color = primaryColor.copy(alpha = focusAnimAlpha.value * 0.4f),
                            radius = focusAnimRadius.value,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = focusAnimAlpha.value),
                            radius = focusAnimRadius.value * 0.6f,
                            center = Offset(centerX, centerY),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = focusAnimAlpha.value),
                            radius = 4.dp.toPx(),
                            center = Offset(centerX, centerY)
                        )
                    }
                }
            }

            // 右上方微型傾斜姿態儀 (Tilt Level Indicator)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .testTag("tilt_indicator")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // 基準外圈
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius * 0.85f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                    )
                    // 中心準心
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 2.dp.toPx(),
                        center = center
                    )

                    // 動態傾斜氣泡
                    val bubbleOffset = Offset(
                        center.x + (motionState.tiltX * radius * 0.65f).coerceIn(-radius * 0.7f, radius * 0.7f),
                        center.y + (motionState.tiltY * radius * 0.65f).coerceIn(-radius * 0.7f, radius * 0.7f)
                    )
                    drawCircle(
                        color = Color(0xFF4E95FF),
                        radius = 4.dp.toPx(),
                        center = bubbleOffset
                    )
                }
            }
        }
    }
}
