 
# API 接口文档

版本：V2.0.1

## 一、接口描述

该文档作为第二阶段开发接口文档，主要包括AI成文接口、AI p图接口、照片手账接口。 
-基础域名：http://1.95.125.238:9001
-请求方式：POST
-Content-Type：multipart/form-data
-通用响应格式：

```json
{
    "code": 200,
    "msg": "success",
    "data": {
    }
}
```

## 二、接口列表

### 1. AI成文接口
  
#### 接口描述

前端上传单张 / 多张照片及成文需求（预设 / 自定义），后端结合大模型视觉分析与平台专属语境规则，一次性返回 3条不同切入点（风格一致但视角不同）的备选文案，供用户挑选或轮播展示。

#### 接口地址

/ai/write

#### 请求参数（Form-Data 表单字段）

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
sessionId|string|是|会话唯一标识，由前端生成并透传，用于追踪本次请求|0912610c-e849-4a97-893d-2ed37c81beca|
image|file[]|是|上传的照片（支持传多张同名表单字段以构成数组）|[二进制文件流]|
requirement|object|否|成文需求（需将下面的 requirement 对象序列化为 JSON 字符串后传入）|{"type":"小红书", "weather":"晴天"}|

requirement 参数说明：

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
type|string|否|成文发布类型/平台（后端内置专属排版规则）|朋友圈/小红书/抖音/Instagram|
location|string|否|拍摄地点（建议读取安卓系统GPS）|海边|
weather|string|否|拍摄时的天气（建议读取天气API）|晴朗微风/阵雨|
emotion|string|否|基调情感|快乐/松弛感/孤独感|
theme|string|否|核心主题|旅行/OOTD/探店|
style|string|否|文字风格|幽默/文艺/王家卫电影风|
length|string|否|篇幅要求|极简短句/适中/长文|
special|string|否|特殊情境/要求|毕业季/生日|
custom|string|否|用户的额外自定义叮嘱（最高优先级）|请夸一下这套新买的咖啡色风衣|

完整示例：
```json
{
    "sessionId": "test-session-001",
    "image": [file1.jpg, file2.jpg],
    "requirement": {
        "type": "朋友圈",
	"location": "桂林日月双塔",
	"weather": "夜风微凉",
        "emotion": "松弛感",
        "theme": "旅行",
        "style": "文艺",
        "length": "短文"
    }
}
```

#### 响应参数

参数名|参数类型|描述|示例
---|---|---|---
code|int|响应码|200|
msg|string|响应信息|success|
content|string[]|文案数组。包含3条独立生成的备选文案，支持内部换行符(\n)| ["文案一...", "文案二...", "文案三..."]|

完整成功响应示例：
{
    "code": 200,
    "msg": "success",
    "data": {
        "content": [
            "夜风微凉，塔影摇金。\n站在水边，像停在时间的褶皱里——\n日月双塔亮着，温柔托住此刻的我。\n桂林的夜，真好啊。🌙",
            
            "不用去追赶时间，时间在这里停下了脚步。\n夜风吹过桂林的双塔，吹散了背包客一整天的疲惫。\n今晚的晚风，是满分的治愈系。✨",
            
            "人生建议：一定要在微凉的夜里来看看日月双塔。\n背包轻，脚步慢，笑得随意。\n旅游从来不是打卡，而是让心松开一寸。🎒"
        ]
    }
}

### 2. AI p图接口

#### 接口描述

前端上传单张照片 + p图需求（预设 / 自定义），后端返回p图后的照片。

#### 接口地址

/ai/picture

#### 请求参数

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
sessionId|string|是|会话唯一标识，由前端生成并透传，用于追踪本次请求|0912610c-e849-4a97-893d-2ed37c81beca
image|file[]|是|上传的照片| |
requirement|object|是|p图需求|{"filter": "复古", "portrait": "瘦脸","background": "去人群",special": "衣服穿上西装"}

requirement 参数说明：
注意：用户至少要选择一个参数

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
filter|string|否|滤镜|复古
portrait|string|否|人像|瘦脸
background|string|否|背景|去人群
special|string|否|特殊要求|衣服穿上西装

完整示例：
```json
{
    "sessionId": "0912610c-e849-4a97-893d-2ed37c81beca",
    "image": [
        {
        }
    ],
    "requirement": {
        "filter": "复古",
        "special": "人群p掉"
    }
}
```

#### 响应参数

参数名|参数类型|描述|示例
---|---|---|---
code|int|响应码|200
msg|string|响应信息|success
image|file|p图后的照片| "p图后的照片"