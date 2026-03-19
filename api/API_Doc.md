
# API 接口文档

版本：V2.0.0

## 一、接口描述

该文档作为第二阶段开发接口文档，主要包括AI成文接口、AI p图接口、照片手账接口。所有接口均采用 JSON 格式 进行数据交互，接口返回统一为 JSON 格式。

-基础域名：http://1.95.125.238:9001
-请求方式：POST
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

前端上传单张 / 多张照片 + 成文需求（预设 / 自定义），后端返回完整的文字内容（如图片描述、文案、故事等）。

#### 接口地址

/ai/write

#### 请求参数

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
sessionId|string|是|会话唯一标识，由前端生成并透传，用于追踪本次请求|0912610c-e849-4a97-893d-2ed37c81beca
image|file[]|是|上传的照片| |
requirement|object|否|成文需求|{ "type": "朋友圈", "emotion": "快乐", "theme": "旅行", "style": "幽默", "length": "短文", "special": "生日", "custom": "自定义文案" }

requirement 参数说明：

参数名|参数类型|是否必填|描述|示例
---|---|---|---|---
type|string|否|成文类型|朋友圈
emotion|string|否|情感|快乐
theme|string|否|主题|旅行
style|string|否|风格|幽默
length|string|否|长度|短文
special|string|否|特殊要求|生日
custom|string|否|自定义文案|自定义文案

完整示例：
```json
{
    "sessionId": "0912610c-e849-4a97-893d-2ed37c81beca",
    "image": [
        {
        },
        {
        }
    ],
    "requirement": {
        "type": "朋友圈",
        "emotion": "快乐",
        "theme": "旅行",
        "style": "幽默",
        "length": "短文",
        "special": "生日",
        "custom": "自定义文案"
    }
}
```

#### 响应参数

参数名|参数类型|描述|示例
---|---|---|---
code|int|响应码|200
msg|string|响应信息|success
content|string|成文内容| "清晨的阳光洒在窗台，猫咪蜷在一旁打盹"

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