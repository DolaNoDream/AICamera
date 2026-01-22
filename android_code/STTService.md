# 语音转文字功能类使用说明文档

## 1.模块概述

本模块提供语音转文字功能，支持将语音转为文字。

## 2.文件结构说明

```plaintext
com.example.aicamera.STT
|—— SparkAsrManager.java    #STT核心实现类
```

## 3.依赖和权限说明

该模块依赖于星火公司提供的.aar文件，因此需要在'app/libs'目录下添加对应的.aar文件:Codec.aar、SparkChain.aar

同时需要在'app/build.gradle'中添加以下依赖：

```groovy
//引用libs下的.aar文件
implementation files('libs/Codec.aar')
implementation files('libs/SparkChain.aar')

implementation("androidx.appcompat:appcompat:1.6.1")
implementation("com.google.android.material:material:1.9.0")
    // SDK 内部可能用到的基础库
implementation("com.google.code.gson:gson:2.10.1")
```

由于需要使用麦克风，需要在'manifest.xml'中添加以下权限：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## 4.使用说明

### 4.1 导入包

在需要使用语音转文字功能的类中，导入以下包：

```java
import com.example.aicamera.STT.SparkAsrManager;
```

### 4.2 初始化

在应用启动时，需要在onCreate()中初始化STT模块，代码如下：

```java
SparkAsrManager.getInstance().init(this);
```

### 4.3 开始语音转文字

在需要开始语音转文字的地方，调用以下代码：

```java
 // 开始录音
SparkAsrManager.getInstance().startListening(new SparkAsrManager.AsrListener() {
    @Override
    public void onResult(String text, boolean isLast) {
    }

    @Override
    public void onError(int code, String msg) {
        Log.e("ASR", "错误: " + msg);
    }
});
```

调用SparkAsrManager.getInstance().startListening()方法开始录音，并传入一个实现了AsrListener接口的回调函数，用于处理语音转文字的结果。
AsrListener接口有两个方法：onResult()和onError()，分别用于处理语音转文字的结果和错误信息。需要重写这两个抽象方法。onResult()方法在语音转文字成功时被调用，参数text为转换后的文字，isLast表示是否是最后一段文字。onError()方法在语音转文字失败时被调用，参数code为错误码，msg为错误信息。
具体参数说明如下：

| 参数名 | 类型   | 说明                                                         |
| ------ | ------ | ------------------------------------------------------------ |
| text   | String | 转换后的文字                                                 |
| isLast | boolean | 是否是最后一段文字，如果为true，表示录音结束，如果为false，表示还有后续文字 |
| code   | int    | 错误码                                                       |
| msg    | String | 错误信息                                                     |

调用示例：

```java
SparkAsrManager.getInstance().startListening(new SparkAsrManager.AsrListener() {
    @Override
    public void onResult(String text, boolean isLast) {
        Log.d("ASR", "识别结果: " + text);
        if (isLast) {
            // 录音结束
            Log.d("ASR", "录音结束");
        }
    }

    @Override
    public void onError(int code, String msg) {
        Log.e("ASR", "错误: " + msg);
    }
});
```

### 4.4 停止语音转文字

在需要停止语音转文字的地方，调用以下代码：

```java
SparkAsrManager.getInstance().stopListening();
```

调用SparkAsrManager.getInstance().stopListening()方法停止录音。

### 4.5 销毁

在应用退出时，需要在onDestroy()中销毁STT模块，代码如下：

```java
SparkAsrManager.getInstance().destroy();
```

## 5.注意事项

- 在使用语音转文字功能时，需要确保设备有麦克风权限，否则无法录音。
- 本模块没有显示申请麦克风权限的代码，需要在应用启动时手动申请权限，或手动增加申请权限代码。
- 需要将Codec.aar和SparkChain.aar文件添加到libs目录下，并在build.gradle中添加依赖。