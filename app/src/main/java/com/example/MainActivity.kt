package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.DepthPhoto
import com.example.ui.components.ProcessingOverlay
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ViewerScreen
import com.example.ui.theme.Depth3DTheme
import com.example.viewmodel.Depth3DViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppScreen {
    GALLERY,
    VIEWER
}

class MainActivity : ComponentActivity() {

    private val viewModel: Depth3DViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Depth3DTheme(dynamicColor = true) {
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                var currentScreen by remember { mutableStateOf(AppScreen.GALLERY) }
                val activePhoto by viewModel.activePhoto.collectAsState()
                val processingState by viewModel.processingState.collectAsState()

                // 攔截並顯示 ViewModel 發出的訊息
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }

                // 系統返回鍵處理
                if (currentScreen == AppScreen.VIEWER) {
                    BackHandler {
                        currentScreen = AppScreen.GALLERY
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState == AppScreen.VIEWER) {
                                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                    )
                                } else {
                                    (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                                        slideOutHorizontally { width -> width } + fadeOut()
                                    )
                                }
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.GALLERY -> {
                                    GalleryScreen(
                                        viewModel = viewModel,
                                        onPhotoSelected = { photo ->
                                            currentScreen = AppScreen.VIEWER
                                        }
                                    )
                                }
                                AppScreen.VIEWER -> {
                                    activePhoto?.let { photo ->
                                        ViewerScreen(
                                            viewModel = viewModel,
                                            photo = photo,
                                            onBackClick = {
                                                currentScreen = AppScreen.GALLERY
                                            }
                                        )
                                    } ?: run {
                                        currentScreen = AppScreen.GALLERY
                                    }
                                }
                            }
                        }

                        // 本地端全自動深度推論遮罩對話框
                        ProcessingOverlay(state = processingState)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.motionController.startListening()
    }

    override fun onPause() {
        super.onPause()
        viewModel.motionController.stopListening()
    }
}
