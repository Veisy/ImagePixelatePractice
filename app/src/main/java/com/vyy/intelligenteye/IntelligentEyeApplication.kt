package com.vyy.intelligenteye

import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig

// If your application only uses specific cameras on the device, such as the default front camera,
// you can set CameraX to ignore other cameras,
// which can reduce startup latency for the cameras your application uses. (Android Developer)
class IntelligentEyeApplication : Application(), CameraXConfig.Provider {
    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setAvailableCamerasLimiter(CameraSelector.DEFAULT_BACK_CAMERA)
            .build()
    }
}
