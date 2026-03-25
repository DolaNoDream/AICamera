# 姿势推荐 API 接入模块说明文档 

## 1. 模块概述

本模块封装了“AI 姿势推荐接口”的调用逻辑。通过上传一张照片及可选的拍摄意图，模块会异步返回 AI 生成的姿势示意图 URL、构图建议以及详细的肢体动作指导。

## 2. 文件结构与作用

该模块采用 Retrofit2 + Gson 架构实现，文件存放在 api 包下：

```plaintext
com.example.aicamera.network
├── PoseRecommendationClient.java   # 对外暴露的核心调用类（单例），处理业务逻辑与参数构建
├── PoseApiService.java             # Retrofit 接口定义，映射服务端 RESTful 接口
└── model                           # 数据实体类包
    ├── PoseResponse.java           # 接口顶层响应模型
    ├── PoseSuggestion.java         # 单个姿势建议模型
    └── PoseDetails.java            # 姿势细节模型（头、臂、腿等部位具体要求）
```

## 3.依赖说明

请在项目build.gradle中添加以下依赖：

```groovy
    // 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
```

## 4. 接口调用说明
PoseRecommendationClient
这是本模块的唯一入口类，通过 'getInstance()' 方法获取单例。

方法签名：
```java
public void analyzePose(File imageFile, String userIntent, String metaJson, PoseCallback callback)
```

参数说明：

|参数名|类型|必填|说明|
|:---|:---|:---|:---|
|imageFile|File|是|用户拍摄的图片文件|
|userIntent|String|否|用户拍摄意图，例如“拍照”、“跳舞”等|
|metaJson|String|否|元数据 JSON 字符串，包含拍摄时间、地点等信息|
|callback|PoseCallback|是|回调接口，用于处理服务端返回的数据|


## 5.使用示例

```java
// 1. 获取客户端实例
PoseRecommendationClient client = PoseRecommendationClient.getInstance();

// 2. 准备图片文件（确保已有读取权限）
File imageFile = new File(getContext().getCacheDir(), "input.jpg");

// 3. 发起调用
client.analyzePose(imageFile, "可爱风 坐姿", null, new PoseRecommendationClient.PoseCallback() {
    @Override
    public void onSuccess(PoseResponse response) {
        // 成功处理逻辑
        String imageUrl = response.poseImageUrl; // 拿到的示意图URL
        String guide = response.guideText;       // 拿到的指导文字
        // 渲染 UI...
    }

    @Override
    public void onError(String errorMessage) {
        // 错误处理逻辑（网络超时、服务器500等）
        Log.e("API", "Error: " + errorMessage);
    }
});
```

## 6.返回内容详细说明
接口成功返回后，会通过 PoseCallback 回调接口返回一个 PoseResponse 对象，其中包含以下字段：

|字段名|类型|说明|
|:---|:---|:---|
|sessionId|String|会话ID，用于唯一标识一次请求|
|poseImageUrl|String|生成的姿势示意图URL|
|guideText|String|详细的指导文字|
|voiceAudioText|String|语音指导文字|
|poseSuggestions|List<PoseSuggestion>|多个姿势建议|

每个 PoseSuggestion 包含以下字段：
每个建议包含 name (名称) 和 details (各部位动作)。

details 内部字段：head (头), arms (臂), hands (手), torso (躯干), legs (腿), feet (脚), orientation (朝向)。

## 7.响应内容的JSON实例
```json
{
    "sessionId": "uuid-123456",
    "poseImageUrl": "http://1.95.125.238:9001/images/res_01.jpg",
    "guideText": "请将人物置于画面中心，镜头稍稍下移。",
    "poseSuggestions": [
        {
            "name": "前倾倚车姿态",
            "details": {
                "head": "头部微侧",
                "legs": "左腿微屈，右腿伸直"
            },
            "tips": ["双手轻放车头", "身体前倾"]
        }
    ]
}
```

## 8. 注意事项
- HTTP 配置：由于后端使用 HTTP 协议，请在 AndroidManifest.xml 的 <application> 标签中设置 android:usesCleartextTraffic="true"。
- 超时设置：AI 模型处理较慢，模块内部默认 readTimeout 为 120秒。
- 权限：确保 App 拥有 INTERNET 权限。
