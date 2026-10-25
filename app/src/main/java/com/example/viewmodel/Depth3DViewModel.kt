package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SamplePhotosProvider
import com.example.engine.LocalDepthEstimator
import com.example.engine.MotionController
import com.example.engine.ParallaxEngine
import com.example.model.DepthPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Processing(val stepTitle: String, val progress: Float) : ProcessingState()
    object Complete : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}

data class TapFocusEvent(
    val x: Float,
    val y: Float,
    val newDepth: Float,
    val timestamp: Long = System.currentTimeMillis()
)

class Depth3DViewModel(application: Application) : AndroidViewModel(application) {

    val motionController = MotionController(application.applicationContext, viewModelScope)
    val parallaxEngine = ParallaxEngine()

    private val _photos = MutableStateFlow<List<DepthPhoto>>(emptyList())
    val photos: StateFlow<List<DepthPhoto>> = _photos.asStateFlow()

    private val _activePhoto = MutableStateFlow<DepthPhoto?>(null)
    val activePhoto: StateFlow<DepthPhoto?> = _activePhoto.asStateFlow()

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private val _focusPlane = MutableStateFlow(0.65f)
    val focusPlane: StateFlow<Float> = _focusPlane.asStateFlow()

    private val _parallaxStrength = MutableStateFlow(1.0f)
    val parallaxStrength: StateFlow<Float> = _parallaxStrength.asStateFlow()

    private val _bokehRadius = MutableStateFlow(8.0f)
    val bokehRadius: StateFlow<Float> = _bokehRadius.asStateFlow()

    private val _showDepthMapOnly = MutableStateFlow(false)
    val showDepthMapOnly: StateFlow<Boolean> = _showDepthMapOnly.asStateFlow()

    private val _tapFocusEvent = MutableStateFlow<TapFocusEvent?>(null)
    val tapFocusEvent: StateFlow<TapFocusEvent?> = _tapFocusEvent.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        loadInitialPresets()
        motionController.startListening()
    }

    private fun loadInitialPresets() {
        viewModelScope.launch(Dispatchers.Default) {
            val presets = SamplePhotosProvider.getSamplePhotos()
            _photos.value = presets
            if (presets.isNotEmpty()) {
                selectPhoto(presets[0])
            }
        }
    }

    fun selectPhoto(photo: DepthPhoto) {
        _activePhoto.value = photo
        _focusPlane.value = photo.focusPlane
        _parallaxStrength.value = photo.parallaxMultiplier
        _bokehRadius.value = photo.bokehRadius
        _showDepthMapOnly.value = false

        // 預先在背景執行緒準備圖層渲染快取
        viewModelScope.launch(Dispatchers.Default) {
            parallaxEngine.prepareLayers(
                photoId = photo.id,
                source = photo.originalBitmap,
                depthMap = photo.depthBitmap,
                focusPlane = photo.focusPlane,
                bokehRadius = photo.bokehRadius
            )
        }
    }

    fun setFocusPlane(plane: Float) {
        val clamped = plane.coerceIn(0.0f, 1.0f)
        _focusPlane.value = clamped
        refreshEngineLayers()
    }

    fun setParallaxStrength(strength: Float) {
        _parallaxStrength.value = strength.coerceIn(0.3f, 2.2f)
    }

    fun setBokehRadius(radius: Float) {
        _bokehRadius.value = radius.coerceIn(0.0f, 20.0f)
        refreshEngineLayers()
    }

    fun toggleShowDepthMapOnly() {
        _showDepthMapOnly.value = !_showDepthMapOnly.value
    }

    /**
     * 點擊相片任意點進行對焦 (Tap-to-Focus)
     */
    fun tapToFocus(normX: Float, normY: Float) {
        val current = _activePhoto.value ?: return
        val depthBmp = current.depthBitmap

        val px = (normX * depthBmp.width).toInt().coerceIn(0, depthBmp.width - 1)
        val py = (normY * depthBmp.height).toInt().coerceIn(0, depthBmp.height - 1)

        val pixel = depthBmp.getPixel(px, py)
        val sampledDepth = (Color.red(pixel) / 255f).coerceIn(0.0f, 1.0f)

        _focusPlane.value = sampledDepth
        _tapFocusEvent.value = TapFocusEvent(normX, normY, sampledDepth)

        refreshEngineLayers()
    }

    private fun refreshEngineLayers() {
        val photo = _activePhoto.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            parallaxEngine.prepareLayers(
                photoId = photo.id,
                source = photo.originalBitmap,
                depthMap = photo.depthBitmap,
                focusPlane = _focusPlane.value,
                bokehRadius = _bokehRadius.value
            )
        }
    }

    /**
     * 100% 本地端離線處理使用者新匯入的相片
     */
    fun processImportedPhoto(rawBitmap: Bitmap, customTitle: String = "我的 3D 空間相片") {
        viewModelScope.launch {
            _processingState.value = ProcessingState.Processing("初始化本地神經特徵提取...", 0.15f)
            delay(200)

            _processingState.value = ProcessingState.Processing("多尺度色彩顯著性分析中...", 0.40f)
            delay(250)

            _processingState.value = ProcessingState.Processing("透視空間先驗與雙邊緣濾波...", 0.70f)
            delay(250)

            val result = withContext(Dispatchers.Default) {
                LocalDepthEstimator.estimateDepth(rawBitmap)
            }

            _processingState.value = ProcessingState.Processing("生成立體多圖層與遮擋邊界外推...", 0.90f)
            delay(150)

            val newPhoto = DepthPhoto(
                id = UUID.randomUUID().toString(),
                title = customTitle,
                subtitle = "自訂 3D 照片 · 本地端純離線生成",
                originalBitmap = rawBitmap,
                depthBitmap = result.depthMap,
                focusPlane = result.suggestedFocusPlane,
                parallaxMultiplier = 1.0f,
                bokehRadius = 8.0f,
                isPreset = false
            )

            val updatedList = listOf(newPhoto) + _photos.value
            _photos.value = updatedList
            selectPhoto(newPhoto)

            _processingState.value = ProcessingState.Complete
            delay(300)
            _processingState.value = ProcessingState.Idle
            _toastMessage.emit("成功生成 3D 空間相片！")
        }
    }

    fun saveCurrentAdjustments() {
        val current = _activePhoto.value ?: return
        val updated = current.copy(
            focusPlane = _focusPlane.value,
            parallaxMultiplier = _parallaxStrength.value,
            bokehRadius = _bokehRadius.value
        )
        _activePhoto.value = updated
        _photos.value = _photos.value.map { if (it.id == updated.id) updated else it }
        viewModelScope.launch {
            _toastMessage.emit("已儲存目前 3D 景深參數！")
        }
    }

    fun resetProcessingState() {
        _processingState.value = ProcessingState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        motionController.stopListening()
    }
}
