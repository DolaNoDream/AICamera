# TTS语音合成说明文档

## 1.模块概述

TTS模块用于将文字转换为语音，并输出到指定的音频设备。

## 2.文件结构说明

```plaintext
com.example.aicamera
|—— MyApplication.java      // 全局初始化入口。负责整个 App 生命周期内星火 SDK 的唯一一次初始化，避免 ASR/TTS 冲突。
|—— TTS
    |—— TTSManager.java     // 语音合成管理类。封装了 SDK 的实例化、回调监听、音频流处理、开始/停止合成等逻辑。
```

## 3.依赖与权限说明

### 3.1 SDK导入

与STT一样，该模块依赖于星火公司提供的.aar文件，因此需要在'app/libs'目录下添加对应的.aar文件:Codec.aar、SparkChain.aar

同时需要在'app/build.gradle'中添加以下依赖：

```groovy
//引用libs下的.aar文件
implementation files('libs/Codec.aar')
implementation files('libs/SparkChain.aar')
```

### 3.2 权限声明

在'app/src/main/AndroidManifest.xml'中添加以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

同时，为了全局初始SDK，需要在AndroidManifest.xml中添加以下代码：

```xml
<application
    android:name=".MyApplication"
    ...>
    ...
</application>
```

## 4.使用说明

### 4.1 导入包

在需要使用语音转文字功能的类中，导入以下包：

```java
import com.example.aicamera.TTS.TTSManager;
import com.iflytek.sparkchain.core.tts.TTS;
```

### 4.2 初始化

在应用启动时，需要在onCreate()中初始化STT模块，代码如下：

```java
// 初始化 TTSManager 实例（此时全局已由 MyApplication 初始化）
TTSManager.getInstance().init();
```

### 4.3 开始合成语音

在需要合成语音的代码中，调用以下代码：

```java
TTSManager.getInstance().startTTS(String text, new TTSManager.TTSListener(){
    @Override
    public void onResult(TTS.TTSResult result, Object usrTag) {
        // 回调成功后执行的处理
    }

    @Override
    public void onError(TTS.TTSError error, Object usrTag) {
        // 回调失败后执行的处理
    }
});
```

当调用该代码后，自动会播放合成后的语音，不需要额外的处理。如果需要对合成的语音进行进一步处理，可以在回调中获取TTSResult对象，该对象包含了合成的音频数据。
TTSResult对象结构说明：

|方法|返回值类型|说明|
|---|---|---|
|getData() |byte[] |⾳频数据，最⼩尺⼨:0B, 最⼤尺⼨:10485760B|
|getLen() |int |音频数据长度|
|getStatus() |int |数据状态，0:开始, 1:开始, 2:结束|
|getCed() |String |流式音频数据的进度尾端点|
|getSid() |String |本次会话的id|

TTSError对象结构说明：

|方法|返回值类型|说明|
|---|---|---|
|getCode() |int |错误码|
|getErrMsg() |String |错误信息|
|getSid() |String |本次会话的id|

调用示例：

```java
TTSManager.getInstance().startTTS(text, new TTSManager.TTSListener() {
    @Override
    public void onResult(TTS.TTSResult result, Object usrTag) {
        // 合成并播放成功
        runOnUiThread(() -> {
            //相关按钮的操作
            btnStart.setEnabled(true);
            btnStart.setText("开始合成并播放");
        });
    }

    @Override
    public void onError(TTS.TTSError error, Object usrTag) {
        runOnUiThread(() -> {
            //相关按钮的操作
            btnStart.setEnabled(true);
            btnStart.setText("开始合成并播放");
            Toast.makeText(MainActivity.this, "合成错误: " + error.getErrMsg(), Toast.LENGTH_LONG).show();
        });
    }
});
```

### 4.4 停止合成语音

在需要停止合成语音的代码中，调用以下代码：

```java
TTSManager.getInstance().stopTTS();
```

### 4.5 销毁

在应用退出时，需要在onDestroy()中销毁STT模块，代码如下：

```java
TTSManager.getInstance().destroy();
```

## 5.注意事项

- 在开始合成语音前，需要先获取到待合成的温本（来自服务器），传入到TTSManager中，才能开始合成语音。
- 合成语音是自动播放，调用扬声器的逻辑已经封装，不需要额外操作。
- 发言人权限：默认使用x4_xiaoyan(讯飞小燕)，如需设置其他发言人，目前需要在TTSManager中修改代码，设置发言人。后续可能会提供接口，允许用户自行设置发言人。
- 合成语音的音频格式为raw，采样率16K，单声道，16位位深。
