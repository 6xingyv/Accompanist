package com.mocharealm.accompanist.sample.ui.utils.modifier

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

fun Modifier.deviceRotation(
    rotationFactor: Float = 0.2f,
    cameraDistance: Float = 12f
): Modifier = composed {

    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val rotationSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    if (rotationSensor == null) {
        return@composed this
    }

    // 俯仰角和翻滚角
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sensorManager, rotationSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    // 从旋转矢量获取旋转矩阵
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    // 从旋转矩阵获取方向角（方位角、俯仰角、翻滚角）
                    val orientationAngles = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)

                    // 将弧度转换为角度并更新状态
                    // orientationAngles[1] 是俯仰角 (pitch)，对应 X 轴旋转
                    // orientationAngles[2] 是翻滚角 (roll)，对应 Y 轴旋转
                    pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    this.graphicsLayer {
        // 对 pitch 取反可以获得更自然的效果（手机顶部向下倾斜时，UI顶部向远处旋转）
        rotationX = -pitch * rotationFactor
        rotationY = roll * rotationFactor

        // 设置相机距离增强 3D 视差效果
        this.cameraDistance = cameraDistance * density
    }
}