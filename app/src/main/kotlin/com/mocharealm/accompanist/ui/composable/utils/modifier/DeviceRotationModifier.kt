package com.mocharealm.accompanist.ui.composable.utils.modifier

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
    // composed 块是创建有状态 Modifier 的正确方式。
    // 它能确保每个使用此 Modifier 的组件都有自己的状态实例。

    // 1. 获取 SensorManager 和 旋转矢量传感器
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val rotationSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    // 如果设备没有旋转矢量传感器，则直接返回原始 Modifier，不做任何处理。
    if (rotationSensor == null) {
        return@composed this
    }

    // 2. 创建状态来保存计算出的俯仰角和翻滚角
    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    // 3. 使用 DisposableEffect 来管理传感器的生命周期
    DisposableEffect(sensorManager, rotationSensor) {
        // 创建传感器事件监听器
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

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // 在这个场景下我们不需要处理精度变化
            }
        }

        // 注册监听器
        sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_UI // 使用适合UI的更新频率
        )

        // onDispose 块是 DisposableEffect 的核心，它会在 Composable 离开组合时执行
        onDispose {
            // 注销监听器以防止资源泄漏
            sensorManager.unregisterListener(listener)
        }
    }

    // 4. 应用 graphicsLayer，将状态化的角度值应用到视觉变换上
    this.graphicsLayer {
        // 乘以一个系数来控制旋转的“轻微”程度
        // 对 pitch 取反可以获得更自然的效果（手机顶部向下倾斜时，UI顶部向远处旋转）
        rotationX = -pitch * rotationFactor
        rotationY = roll * rotationFactor

        // 设置相机距离可以增强 3D 视差效果
        this.cameraDistance = cameraDistance * density
    }
}