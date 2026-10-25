package com.example.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 體感姿態控制器 (Motion Controller)
 *
 * 整合 Android 陀螺儀/旋轉向量感測器、觸控手勢滑動與自動 3D 搖擺視角，
 * 提供絲滑流暢 (60fps) 的多視角視差座標追蹤。
 */
class MotionController(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    data class MotionState(
        val tiltX: Float = 0f, // -1.0 (向左傾斜) ~ 1.0 (向右傾斜)
        val tiltY: Float = 0f, // -1.0 (向上傾斜) ~ 1.0 (向下傾斜)
        val isSensorAvailable: Boolean = false,
        val isSensorActive: Boolean = true,
        val isAutoWiggleActive: Boolean = false
    )

    private val _motionState = MutableStateFlow(MotionState())
    val motionState: StateFlow<MotionState> = _motionState.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private var rotationSensor: Sensor? = null

    // 平滑低通濾波參數
    private var filteredTiltX = 0f
    private var filteredTiltY = 0f
    private val filterAlpha = 0.18f

    // 手指觸控偏移
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    // 零點校正基準 (Zero Calibration)
    private var basePitch = 0f
    private var baseRoll = 0f
    private var isCalibrated = false

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var wiggleJob: Job? = null

    init {
        rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val available = rotationSensor != null
        _motionState.value = _motionState.value.copy(isSensorAvailable = available)
    }

    fun startListening() {
        rotationSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        stopAutoWiggle()
    }

    /**
     * 重設零點基準（將目前手持姿勢視為正中心）
     */
    fun calibrateCenter() {
        isCalibrated = false
        touchOffsetX = 0f
        touchOffsetY = 0f
        filteredTiltX = 0f
        filteredTiltY = 0f
        _motionState.value = _motionState.value.copy(tiltX = 0f, tiltY = 0f)
    }

    /**
     * 觸控手勢滑動視角更新
     */
    fun onTouchDrag(deltaX: Float, deltaY: Float, viewWidth: Float, viewHeight: Float) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        touchOffsetX = (touchOffsetX + (deltaX / viewWidth) * 2.2f).coerceIn(-1.2f, 1.2f)
        touchOffsetY = (touchOffsetY + (deltaY / viewHeight) * 2.2f).coerceIn(-1.2f, 1.2f)
        updateCombinedTilt()
    }

    /**
     * 手指放開時緩慢平滑歸位 (Spring return)
     */
    fun onTouchRelease() {
        scope.launch(Dispatchers.Default) {
            val steps = 12
            for (i in 1..steps) {
                touchOffsetX *= 0.78f
                touchOffsetY *= 0.78f
                updateCombinedTilt()
                delay(16)
            }
            touchOffsetX = 0f
            touchOffsetY = 0f
            updateCombinedTilt()
        }
    }

    /**
     * 切換自動 3D 搖擺巡航模式 (適用於電腦模擬器或靜置展示)
     */
    fun toggleAutoWiggle() {
        val current = _motionState.value.isAutoWiggleActive
        if (current) {
            stopAutoWiggle()
        } else {
            startAutoWiggle()
        }
    }

    private fun startAutoWiggle() {
        stopAutoWiggle()
        _motionState.value = _motionState.value.copy(isAutoWiggleActive = true)
        wiggleJob = scope.launch(Dispatchers.Default) {
            var angle = 0.0
            val speed = 0.065
            while (isActive) {
                angle += speed
                if (angle > 2 * PI) angle -= 2 * PI
                val wiggleX = (sin(angle) * 0.75f).toFloat()
                val wiggleY = (cos(angle * 0.5) * 0.25f).toFloat()

                _motionState.value = _motionState.value.copy(
                    tiltX = wiggleX,
                    tiltY = wiggleY
                )
                delay(16) // ~60fps
            }
        }
    }

    private fun stopAutoWiggle() {
        wiggleJob?.cancel()
        wiggleJob = null
        _motionState.value = _motionState.value.copy(isAutoWiggleActive = false)
        updateCombinedTilt()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _motionState.value.isAutoWiggleActive) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            // azimuth = orientationAngles[0], pitch = orientationAngles[1], roll = orientationAngles[2]
            val pitch = orientationAngles[1]
            val roll = orientationAngles[2]
            processAngles(pitch, roll)
        } else if (event.sensor.type == Sensor.TYPE_GRAVITY || event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // 從重力向量估算傾斜角
            val ax = event.values[0] / 9.81f
            val ay = event.values[1] / 9.81f
            processAngles(ay * 0.8f, ax * 0.8f)
        }
    }

    private fun processAngles(pitch: Float, roll: Float) {
        if (!isCalibrated) {
            basePitch = pitch
            baseRoll = roll
            isCalibrated = true
        }

        // 傾斜相對夾角 (正負 30 度 ~ 0.52 弧度滿量程)
        val maxAngle = 0.52f
        val deltaRoll = (roll - baseRoll).coerceIn(-maxAngle, maxAngle)
        val deltaPitch = (pitch - basePitch).coerceIn(-maxAngle, maxAngle)

        // 左右傾斜對應 X 軸，前後仰角對應 Y 軸
        val targetX = deltaRoll / maxAngle
        val targetY = deltaPitch / maxAngle

        // IIR 低通平滑濾波，消除手部微顫抖
        filteredTiltX = filteredTiltX * (1f - filterAlpha) + targetX * filterAlpha
        filteredTiltY = filteredTiltY * (1f - filterAlpha) + targetY * filterAlpha

        updateCombinedTilt()
    }

    private fun updateCombinedTilt() {
        if (_motionState.value.isAutoWiggleActive) return
        val totalX = (filteredTiltX + touchOffsetX).coerceIn(-1.2f, 1.2f)
        val totalY = (filteredTiltY + touchOffsetY).coerceIn(-1.2f, 1.2f)
        _motionState.value = _motionState.value.copy(
            tiltX = totalX,
            tiltY = totalY
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
