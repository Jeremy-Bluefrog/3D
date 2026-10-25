package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.LocalDepthEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Depth3D", appName)
    }

    @Test
    fun `test local depth estimation runs on-device`() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        testBmp.eraseColor(Color.BLUE)

        val result = LocalDepthEstimator.estimateDepth(testBmp)
        assertNotNull(result)
        assertNotNull(result.depthMap)
        assertEquals(100, result.depthMap.width)
        assertEquals(100, result.depthMap.height)
        assertTrue(result.suggestedFocusPlane in 0.0f..1.0f)
    }
}
