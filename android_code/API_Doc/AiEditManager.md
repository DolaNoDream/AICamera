# AiEditManager接口说明文档

## 一、文件结构

``` plaintext
app/
├── src/main/
    ├── java/com/example/aicamera/
        ├── AiEditManager.java           // 核心工具类（封装接口调用+图片下载）
        ├── AiEditResult.java            // 统一结果返回类
        ├── PictureRequirement.java      // 修图需求参数类
        └── AiEditTestActivity.java      // 测试类（验证接口调用）
```

## 二、参数说明

|参数名|类型|必填|说明|示例|
|---|---|---|---|---|
|sessionId| String| 是 |会话唯一标识，用于接口请求追踪，建议用 UUID 生成| UUID.randomUUID().toString()|
|imageFile| File| 是 |待修图的图片文件，需确保文件存在且可读取 |new File(getExternalFilesDir("images"), "test.jpg")|
|requirement| PictureRequirement| 是 |修图需求参数，详见下文 |new PictureRequirement()|

requirement参数说明：

|参数名|类型|必填|说明|示例|
|---|---|---|---|---|
|requirement.filter| String |否 |修图滤镜类型，如「复古」「清新」「黑白」| "复古"|
|requirement.portrait| String| 否 |人像调整要求，如「瘦脸」「美白」「磨皮」| "瘦脸"|
|requirement.background| String |否| 背景调整要求，如「模糊背景」「更换背景」 |"模糊背景"|
|requirement.special |String |否| 特殊修图要求，如「去掉人群」「增加对比度」| "去掉图片中的文字"|

**注意事项**：

- requirement 至少需填写一个非空字段（滤镜 / 人像 / 背景 / 特殊要求），否则会触发参数校验失败；
- imageFile 需确保路径正确，建议使用应用私有目录（getExternalFilesDir()），避免权限问题。

## 三、接口调用

**步骤 1**：初始化工具类（推荐全局仅初始化一次）

在 MyApplication 或主 Activity 的 onCreate() 中初始化：

```java
运行
// 方式1：在MyApplication中全局初始化
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化工具类单例（传入Application Context，避免内存泄漏）
        AiEditManager.getInstance(this);
    }
}

// 方式2：在Activity中临时初始化
AiEditManager aiEditManager = AiEditManager.getInstance(getApplication());
```

**步骤 2**：准备调用参数

- 生成唯一的 sessionId；
- 指定待修图的图片文件路径；
- 构建修图需求（PictureRequirement 实例）。

**步骤 3**：调用核心方法，处理回调结果

通过 AiEditManager.editImage() 调用接口，在回调中处理成功 / 失败结果。

```java
runOnUiThread(() -> {
    if (result.isSuccess()) {
         // 成功：获取图片保存路径
        String savePath = result.getImageSavePath();
            // 后续操作：显示图片、分享等
    } else {
         // 失败：获取错误信息
        String errorMsg = result.getErrorMsg();
    }
});
```

**步骤 4**：验证结果

成功：获取图片本地保存路径，可用于显示 / 分享 / 上传；

失败：获取错误信息，排查参数 / 网络 / 接口问题。

## 四、示例代码

```java
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 可选：添加自定义布局

        // 1. 初始化工具类
        AiEditManager aiEditManager = AiEditManager.getInstance(getApplication());

        // 2. 准备参数

        String sessionId = UUID.randomUUID().toString(); // 生成唯一sessionId

        // 待修图图片路径（需确保图片存在）
        File testImageFile = new File(getExternalFilesDir("images"), "test.jpg");

        // 构建修图需求
        PictureRequirement requirement = new PictureRequirement();
        requirement.setFilter("复古"); // 设置滤镜
        requirement.setSpecial("去掉图片中的人群"); // 设置特殊修图要求

        // 3. 检查测试图片是否存在
        if (!testImageFile.exists()) {
            String errorMsg = "测试图片不存在！路径：" + testImageFile.getAbsolutePath();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg);
            return;
        }

        // 4. 调用AI修图接口
        aiEditManager.editImage(sessionId, testImageFile, requirement, result -> {
            // 切回主线程更新UI（网络操作在子线程）
            runOnUiThread(() -> {
                if (result.isSuccess()) {

                    // 成功：获取图片保存路径
                    String savePath = result.getImageSavePath();

                    Toast.makeText(MainActivity.this, 
                            "修图成功！图片保存路径：\n" + savePath, 
                            Toast.LENGTH_LONG).show();

                    Log.d(TAG, "修图成功 → 保存路径：" + savePath);
                    // 后续操作：显示图片、分享等

                } else {

                    // 失败：获取错误信息
                    String errorMsg = result.getErrorMsg();

                    Toast.makeText(MainActivity.this, 
                            "修图失败：" + errorMsg, 
                            Toast.LENGTH_SHORT).show();

                    Log.e(TAG, "修图失败 → 原因：" + errorMsg);

                }
            });
        });
    }
}
```

## 五、照片保存地址说明

### 保存路径规则

图片默认保存到应用私有外部存储目录，路径格式：

``` plaintext
/sdcard/Android/data/[应用包名]/files/AiEditedImages/ai_edit_[时间戳].jpg
```

**示例**（包名 com.example.aicamera）：

``` plaintext
/sdcard/Android/data/com.example.aicamera/files/AiEditedImages/ai_edit_1742434857321.jpg
```

### 路径说明

- /sdcard/：安卓设备的外部存储根目录（真机 / 模拟器通用）；
- Android/data/[应用包名]/files/：应用私有目录，无需动态申请文件权限（安卓 10 + 也可直接访问）；
- AiEditedImages/：工具类固定的保存目录名，可在 AiEditManager 中修改 SAVE_DIR_NAME 常量自定义；
- ai_edit_[时间戳].jpg：自动生成的唯一文件名，避免重复覆盖。

### 如何查看保存的图片

#### 方式 1：Android Studio 设备文件管理器

- 打开 Android Studio → 右侧「Device File Explorer」；
- 进入路径：/sdcard/Android/data/[应用包名]/files/AiEditedImages/；
- 右键图片文件 → 「Save As」导出到电脑查看。

#### 方式 2：手机文件管理器

- 打开手机「文件管理」→ 「Android」→ 「data」→ 「[应用包名]」→ 「files」→ 「AiEditedImages」；

- 直接查看 / 打开下载的图片（部分手机需开启「显示隐藏文件」）。

#### 自定义保存路径

若需修改保存目录，可修改 AiEditManager 中的 SAVE_DIR_NAME 常量：

```java
运行
// 原代码
private static final String SAVE_DIR_NAME = "AiEditedImages";
// 自定义修改为
private static final String SAVE_DIR_NAME = "MyAiEditImages";
```
