# CameraEngine

Android Camera Library

## 如何使用

### 初始化

```xml

<androidx.camera.view.PreviewView 
    android:id="@+id/cameraxPreview"
    android:layout_height="match_parent"
    android:layout_width="match_parent" />
```

```kotlin
val cameraManager = CameraManager.init(CameraXImpl(this, this, binding.cameraxPreview))
cameraManager.initCamera(MyCameraConfig(MyCameraConfig.LENS_BACK))
```

### 拍照

```kotlin
val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, UUID.randomUUID().toString())
    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraEngine")
    }
}

// 两种存储方式
// 1. 系统相册目录
val outputFileOptions = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
// 2. 应用私有目录
val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
    File(getExternalFilesDir("haha"), UUID.randomUUID().toString() + ".jpg")
).build()

cameraManager.takePhoto(outputFileOptions)
```

### 切换镜头

```kotlin
// 切换到当前相反的镜头
cameraManager.switchLens()
// 选择镜头
cameraManager.selectLens(lens)
```

## TODO

- 视频拍摄