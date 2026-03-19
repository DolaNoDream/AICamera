# CameraStreamManager.kt 说明文档

## 1. 简介

CameraStreamManager 允许应用在无相机预览界面的情况下，通过后台流（ImageAnalysis）获取图像数据。为了解决静默模式下硬件不主动对焦的问题，本类内置了指令级对焦控制。
位置在/camera/CameraStreamManager.kt

## 2.环境配置

权限声明（AndroidManifest.xml）：

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
```

核心依赖（build.gradle）：

```groovy
val cameraxVersion = "1.3.1"
implementation("androidx.camera:camera-core:$cameraxVersion")
implementation("androidx.camera:camera-camera2:$cameraxVersion")
implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
```

或者

```groovy
implementation(libs.androidx.camera.core)
implementation(libs.androidx.camera.camera2)
implementation(libs.androidx.camera.lifecycle)
implementation(libs.androidx.camera.view)
```

## 3.核心API参考

|方法|类型|说明|
|---|---|---|
|startSilentCamera(lifecycleOwner)|普通方法|启动硬件。仅开启分析流，不显示预览画面。|
|triggerAutoFocus() |挂起函数| 强制对焦。使相机对准画面中心进行对焦和测光。|
|captureSingleFrame() |挂起函数 |截取图像。从当前内存流中提取最新一帧。|
|saveBitmapToJpg(bitmap) |辅助方法 |持久化。将 Bitmap 转换为压缩的 .jpg 文件|

## 4.使用示例

在静默模式下，推荐的调用链是：启动 -> 触发对焦 -> 获取帧。

```kotlin

// 实例化管理器
// remember 确保重组时不会重复创建
val cameraManager = remember { CameraStreamManager(context) }

// 可以通过封装类的方式使用
class MyScanner(private val cameraManager: CameraStreamManager) {

    fun init(lifecycleOwner: LifecycleOwner) {
        // 1. 静默启动，不绑定任何 PreviewView
        cameraManager.startCamera(lifecycleOwner)
    }

    suspend fun takePhotoAction(): File? {
        // 2. 触发中心自动对焦与测光
        val focusOk = cameraManager.triggerAutoFocus()
        Log.d("Scan", "对焦结果: $focusOk")

        // 3. 截取当前帧数据
        val bitmap = cameraManager.captureSingleFrame()

        // 4. 保存为jpg文件
        return cameraManager.saveBitmapToJpg(bitmap)
    }
}

//可以直接使用：
//在MainActivity中,this为LifecycleOwner
//在Compose中，使用LocalLifecycleOwner.current获取生命周期
cameraManager.startSilentCamera(this)
val isFocus = cameraManager.triggerAutoFocus()
val bitmap = cameraManager.captureSingleFrame()
val file = cameraManager.saveBitmapToJpg(bitmap)
```

## 5.注意事项

- 由于移除了 Preview 用例，相机硬件不会自动运行连续对焦（CAF）。triggerAutoFocus() 通过 FocusMeteringAction 强制向底层 Camera2 驱动发送 3A（自动对焦、自动曝光、自动白平衡）指令，确保在点击瞬间画面是清晰的。
- 对焦耗时：triggerAutoFocus() 是耗时操作（通常 500ms~1.5s）。在光线极暗处可能返回 false，此时仍可抓拍，但需注意画质。
- 生命周期：必须传入正确的 LifecycleOwner。当页面销毁或应用切入后台时，相机硬件会自动关闭以释放资源。
- 首帧延迟：硬件冷启动后，前几帧的曝光可能不稳定。建议在 startSilentCamera 后延迟约 500ms 再进行首次抓拍。
- 虽然该类会随着绑定的LifecycleOwner销毁而自动关闭相机，但依然留有stopCamera()方法，用于手动关闭相机。
