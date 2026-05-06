# BailianImageRAG 工具类使用文档

## 概述

`BailianImageRAG` 是一个封装好的阿里云百炼图片RAG检索工具类，用于通过图片和用户需求，检索知识库中匹配的姿势数据。

## 功能特性

- 支持图片路径和二进制数据两种输入方式
- 自动处理图片编码（JPG/PNG/WEBP）
- 返回结构化的匹配结果（包含相似度、姿势详情、场景信息等）
- 支持获取单条最佳匹配或多条结果

## 安装依赖

```bash
pip install dashscope python-dotenv
```

## 环境变量配置

在项目根目录创建 `.env` 文件：

```env
# DashScope API Key（阿里云百炼）
DASHSCOPE_API_KEY=sk-xxx

# 阿里云百炼应用ID
BAILIAN_APP_ID=your_app_id
```

## 快速开始

### 基础用法

```python
from ragAPI.BailianImageRAG import BailianImageRAG

# 初始化工具类（自动读取环境变量）
rag = BailianImageRAG()

# 使用图片路径检索
image_path = "/path/to/your/image.jpg"
query = "请描述这张图片中人物的姿势和动作"

result = rag.retrieve(image_path, query, top_k=3)

if result['success']:
    for item in result['results']:
        print(f"相似度: {item['score']:.4f}")
        print(f"姿势: {item['pose_overall']}")
        print(f"场景: {item['scene_context']['environment']}")
```

### 使用图片二进制数据

```python
from ragAPI.BailianImageRAG import BailianImageRAG

rag = BailianImageRAG()

# 读取图片二进制数据
with open("image.jpg", 'rb') as f:
    image_bytes = f.read()

# 检索
result = rag.retrieve(image_bytes, "分析图片中的人物姿势")
```

### 获取最佳匹配

```python
best_match = rag.retrieve_best_match(image_path, "描述人物动作")
if best_match:
    print(f"最匹配 - 相似度: {best_match['score']:.4f}")
    print(f"姿势详情: {best_match['pose_details']}")
```

## API 参考

### 构造函数

```python
BailianImageRAG(api_key=None, app_id=None)
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| api_key | str | 否 | DashScope API Key，优先使用环境变量 `DASHSCOPE_API_KEY` |
| app_id | str | 否 | 阿里云百炼应用ID，优先使用环境变量 `BAILIAN_APP_ID` |

### retrieve 方法

```python
retrieve(image, query, top_k=5)
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| image | str / bytes | 是 | 图片文件路径或二进制数据 |
| query | str | 是 | 用户需求字符串 |
| top_k | int | 否 | 返回结果数量，默认5 |

**返回值结构：**

```python
{
    'success': bool,           # 是否成功
    'message': str,            # 消息
    'results': List[dict],     # 匹配结果列表（按相似度降序）
    'request_id': str          # 请求ID
}
```

### retrieve_best_match 方法

```python
retrieve_best_match(image, query)
```

返回最匹配的单条结果，如果失败则返回 `None`。

## 返回结果字段说明

每个结果项包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| score | float | 相似度分数（0-1） |
| filename | str | 匹配到的文件名 |
| pose_overall | str | 整体姿势描述 |
| pose_details | dict | 详细姿势信息（头部、手臂、手部、躯干等） |
| scene_context | dict | 场景上下文（环境、道具、风格） |
| camera_composition | dict | 镜头构图（景别、角度） |
| facial_expression | str | 面部表情描述 |

### scene_context 结构

```python
{
    'environment': str,   # 环境描述
    'interaction_props': str,  # 互动道具
    'vibe_style': str     # 风格氛围
}
```

### pose_details 结构

```python
{
    'head': str,     # 头部姿态
    'arms': str,     # 手臂姿态
    'hands': str,    # 手部姿态
    'torso': str,    # 躯干姿态
    'hips': str,     # 髋部姿态
    'legs': str,     # 腿部姿态（如可见）
    'feet': str,     # 脚部姿态（如可见）
    'orientation': str  # 朝向
}
```

## 完整示例

```python
from ragAPI.BailianImageRAG import BailianImageRAG

def analyze_posture(image_path: str, user_query: str):
    """分析图片中的人物姿势"""
    # 初始化
    rag = BailianImageRAG(
        api_key="sk-xxx",
        app_id="your_app_id"
    )
    
    # 执行检索
    result = rag.retrieve(image_path, user_query, top_k=5)
    
    if not result['success']:
        print(f"检索失败: {result['message']}")
        return None
    
    # 处理结果
    matches = []
    for item in result['results']:
        matches.append({
            'similarity': item['score'],
            'posture': item['pose_overall'],
            'environment': item['scene_context'].get('environment', ''),
            'details': item['pose_details']
        })
    
    return matches

# 使用示例
if __name__ == '__main__':
    results = analyze_posture(
        image_path="../image/test.jpg",
        user_query="请分析这张图片中人物的姿势和动作"
    )
    
    for i, match in enumerate(results):
        print(f"\n=== 匹配 #{i+1} ===")
        print(f"相似度: {match['similarity']:.2%}")
        print(f"姿势: {match['posture']}")
        print(f"环境: {match['environment']}")
```

## 错误处理

```python
result = rag.retrieve(image_path, query)

if result['success']:
    # 处理成功结果
    process_results(result['results'])
else:
    # 处理失败
    print(f"错误: {result['message']}")
    print(f"请求ID: {result['request_id']}")
```


## 配置示例（.env 文件）

```env
# .env 文件示例
DASHSCOPE_API_KEY=sk-40ed2e1b81f545308b6a92574e923706
BAILIAN_APP_ID=7a93bb813d09459c92d96b3108f8a700
```

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-05-01 | 初始版本，支持图片检索和结果解析 |


