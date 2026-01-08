# AI Camera 前端 ↔ 总后端 API 文档

**项目名称**：AI Camera（智能拍照辅助应用）  
**接口负责人**：后端组  
**当前版本**：0.1.0（2026年1月）  
**服务器地址**：`http://localhost:8080` （生产环境待替换）  
**接口风格**：RESTful + Multipart/Form-data（图片上传） + JSON  
**主要功能**：接收安卓端相机帧/语音指令 → 调用Python子服务（姿态分析/语音助手/TTS）→ 返回姿势建议、UI提示、语音引导及音频

## 接口总览

| 接口路径              | 方法 | 主要功能                             | 业务场景                 | 重要性 |
|-----------------------|------|--------------------------------------|--------------------------|--------|
| `/api/health`         | GET  | 服务健康检查                         | 监控/上线检查            | ★☆☆☆☆ |
| `/api/frame/analyze`  | POST | 上传单帧图片 → 获取姿势建议+TTS语音  | 相机实时拍照指导         | ★★★★★ |
| `/api/voice/text`     | POST | 上传语音识别文本 → 获取语音助手回复+TTS | 语音交互问答             | ★★★★☆ |

## 公共说明

- 所有接口均使用 `sessionId` 标识同一用户会话（建议由前端生成uuid）
- 图片统一使用 `multipart/form-data` 上传，格式 jpg/png
- 返回数据统一使用 JSON
- 错误响应统一格式（参考最下方 ErrorResponse）
- 语音相关字段（如 `voiceGuideText`、`voiceReplyText`）是专门给TTS使用的，通常比UI显示文本更口语化、简短

## 详细接口说明

### 1. 健康检查

```text
GET  /api/health
```

**功能**：确认后端服务是否正常

**请求参数**：无

**正常响应**（200）

```json
"ok"
```

**建议使用场景**：启动时、监控、心跳检测

### 2. 相机帧分析（核心接口）

```text
POST  /api/frame/analyze
Content-Type: multipart/form-data
```

**功能**：  
上传相机当前帧 → 后端调用PoseAI分析姿势 → 返回推荐姿势、UI叠加提示、语音指导文案及TTS音频地址

**请求参数**

| 字段名      | 类型     | 必填 | 说明                                      | 示例                              |
|-------------|----------|------|-------------------------------------------|-----------------------------------|
| sessionId   | string   | 是   | 会话标识                                  | "s1" 或 uuid                      |
| image       | binary   | 是   | 当前相机帧（jpg/png）                     | —                                 |
| meta        | string   | 否   | 相机元数据（JSON字符串）                  | {"camera":"back","width":1080,"height":1920} |

**成功响应**（200） - FrameAnalyzeResponse

```json5
{
  "sessionId":        "s1",
  "poseSuggestions": [
    {
      "id":       "p1",
      "name":     "侧身抬手",
      "priority": 1,               // 1 = 最高推荐
      "tips":     ["身体侧 30°", "手抬到额头旁", "肩放松"]
    },
    {
      "id":       "p2",
      "name":     "微仰头",
      "priority": 2,
      "tips":     ["下巴抬一点", "眼神看上方", "自然微笑"]
    }
  ],
  "overlay": {
    "textHint":    "推荐姿势：侧身抬手",
    "hintImageUrl": "https://example.com/mock/pose_p1.png"   // 用于UI叠加的参考图
  },
  "voiceGuideText": "现在侧身一点，右手抬起来，肩放松。",   // 给TTS用的口语化引导
  "audioUrl": "http://localhost:9002/files/s1_001.mp3"        // 可直接播放的语音地址（可能为空）
}
```

**推荐前端展示逻辑**：
1. 显示最高优先级（priority=1）的 poseSuggestion 作为主推荐
2. overlay.textHint 叠加在画面上
3. 显示 hintImageUrl 作为参考小图
4. 播放 audioUrl（如果存在）

### 3. 语音助手对话

```text
POST  /api/voice/text
Content-Type: application/json
```

**功能**：  
用户说话 → 前端ASR转文字 → 送给后端 → 后端调用VoiceAI/chat得到回复 → 再调用TTS生成语音 → 返回文字+语音地址

**请求参数**（JSON）

| 字段名    | 类型   | 必填 | 说明                   | 示例                        |
|-----------|--------|------|------------------------|-----------------------------|
| sessionId | string | 是   | 会话标识               | "s1"                        |
| text      | string | 是   | 用户语音识别后的文字   | "我想拍全身照 显腿长"       |

**成功响应**（200） - VoiceTextResponse

```json5
{
  "sessionId":           "s1",
  "assistantReplyText":  "好的，我给你三个显腿长的全身姿势：交叉腿、侧身伸腿、低机位抬下巴。你想更酷还是更温柔？",
  "voiceReplyText":      "好的，我给你三个显腿长的全身姿势。你想更酷还是更温柔？",   // 更口语化，适合TTS
  "audioUrl":            "http://localhost:9002/files/s1_002.mp3"                    // 可直接播放
}
```

**建议前端处理**：
- UI 显示 `assistantReplyText`（较完整）
- 语音播放 `audioUrl`
- 如果用户连续问答，保持同一 `sessionId`

## 错误响应（公共）

所有接口出错时统一返回（HTTP 4xx/5xx）

```json5
{
  "timestamp": "2026-01-07T12:34:56.789Z",
  "status":    400,
  "error":     "Bad Request",
  "message":   "image is required",
  "path":      "/api/frame/analyze"
}
```

常见错误码建议：

- 400 - 参数缺失/格式错误
- 413 - 图片太大
- 500 - 内部服务异常（PoseAI/VoiceAI/TTS调用失败）

## 快速总结 - 前端最常用字段对照表

| 场景               | 主要展示字段             | 播放字段     | 叠加提示字段         |
|--------------------|---------------------------|--------------|----------------------|
| 相机拍照指导       | poseSuggestions + overlay | audioUrl     | overlay.textHint     |
| 语音助手回答       | assistantReplyText        | audioUrl     | —                    |
