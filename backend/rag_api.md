# RAG 服务接口文档

## 概述

RAG（Retrieval-Augmented Generation）服务提供基于向量匹配的知识库检索功能，支持根据输入文本匹配最相关的知识库条目。

## 核心类：RAGManager

### 初始化

```python
from rag import RAGManager

# 默认配置（使用 Hugging Face）
rag = RAGManager(
    store_path="./vector_store",    # 向量库存储路径
    use_modelscope=False,           # 是否使用魔搭社区
    model_name="all-MiniLM-L6-v2"   # 模型名称
)

# 使用魔搭社区
rag = RAGManager(
    store_path="./vector_store",
    use_modelscope=True,
    model_name="damo/nlp_bge_small_zh"
)
```

### 方法列表

| 方法 | 功能 | 参数 | 返回值 |
|------|------|------|--------|
| `build_vector_store()` | 构建向量库 | `json_path`: JSON文件路径 / `data`: 数据列表 | `None` |
| `load_vector_store()` | 加载向量库 | 无 | `bool` (是否成功) |
| `match()` | 匹配查询 | `query`: 查询文本, `top_k`: 返回条数 | `list` |
| `match_with_threshold()` | 按阈值匹配 | `query`: 查询文本, `threshold`: 相似度阈值 | `list` |
| `get_best_match()` | 获取最佳匹配 | `query`: 查询文本 | `dict` 或 `None` |

## 接口使用示例

### 1. 构建向量库

```python
from rag import RAGManager

rag = RAGManager(store_path="./vector_store")

# 从JSON文件构建
rag.build_vector_store(json_path="../ragdata/rag_knowledge_base_v3.json")

# 或从数据列表构建
data = [
    {
        "filename": "img_001.jpg",
        "extracted_info": {
            "scene_context": {"environment": "户外草地"},
            "pose_overall": "人物站立，双臂自然下垂"
        }
    }
]
rag.build_vector_store(data=data)
```

### 2. 加载并匹配

```python
from rag import RAGManager

rag = RAGManager(store_path="./vector_store")

# 加载向量库
if not rag.load_vector_store():
    print("向量库不存在，请先构建")
    rag.build_vector_store(json_path="../ragdata/rag_knowledge_base_v3.json")
    rag.load_vector_store()

# 基础匹配（返回top-k结果）
results = rag.match("户外草地坐着的姿势", top_k=3)
for result in results:
    print(f"相似度: {result['score']:.4f}")
    print(f"文件名: {result['filename']}")
    print(f"姿势: {result['data']['extracted_info']['pose_overall']}")

# 按阈值匹配
results = rag.match_with_threshold("室内站立姿势", threshold=0.5)

# 获取最佳匹配
best_match = rag.get_best_match("海边自拍")
if best_match:
    print(f"最佳匹配: {best_match['filename']} (相似度: {best_match['score']:.4f})")
```

## 返回数据结构

### match() / match_with_threshold() 返回格式

```python
[
    {
        "score": 0.8567,           # 相似度分数 (0-1)
        "filename": "img_047.jpg", # 匹配到的文件名
        "data": {                  # 原始数据
            "filename": "img_047.jpg",
            "extracted_info": {
                "scene_context": {
                    "environment": "户外草地",
                    "interaction_props": "",
                    "vibe_style": "自然清新"
                },
                "camera_composition": {
                    "framing": "全身",
                    "angle": "平视"
                },
                "pose_overall": "人物正面站立，双臂自然下垂",
                "pose_details": {
                    "head": "正视镜头",
                    "arms": "自然下垂",
                    "legs": "双腿分开与肩同宽"
                }
            }
        }
    }
]
```

### get_best_match() 返回格式

```python
{
    "score": 0.8567,
    "filename": "img_047.jpg",
    "data": { ... }
}
# 或 None（无匹配时）
```

## 部署说明

### 环境要求

- Python >= 3.8
- 依赖包：见 `requirements.txt`

### 安装步骤

```bash
# 进入项目目录
cd backend

# 安装依赖
pip install -r requirements.txt

# 首次运行会自动下载模型（约500MB）
# 模型会缓存到用户目录：~/.cache/huggingface/hub/
```

### 服务集成示例

```python
# 在其他服务中集成RAG
from rag import RAGManager

class PhotoRecommendationService:
    def __init__(self):
        self.rag = RAGManager(store_path="./vector_store")
        self.rag.load_vector_store()
    
    def recommend_poses(self, user_query: str, limit: int = 5):
        """根据用户查询推荐拍照姿势"""
        results = self.rag.match(user_query, top_k=limit)
        return [
            {
                "image_path": result["filename"],
                "similarity": result["score"],
                "pose_description": result["data"]["extracted_info"]["pose_overall"],
                "scene": result["data"]["extracted_info"]["scene_context"].get("environment", "")
            }
            for result in results
        ]
```

## 性能说明

| 操作 | 耗时（参考） | 说明 |
|------|-------------|------|
| 模型加载 | 10-20秒 | 首次加载，后续使用缓存 |
| 向量库加载 | <0.01秒 | 加载本地numpy文件 |
| 单次匹配 | 0.01-0.1秒 | 取决于向量库大小 |

## 注意事项

1. **首次运行**：模型会自动下载并缓存，耗时较长（10-20秒）
2. **向量库构建**：只需执行一次，后续直接加载即可
3. **模型选择**：默认使用 `all-MiniLM-L6-v2`（英文模型），中文场景建议使用魔搭社区的中文模型
4. **环境变量**：设置 `HF_TOKEN` 可提高 Hugging Face API 调用限制

## 目录结构

```
backend/
├── rag/                      # RAG核心模块
│   ├── __init__.py          # 导出 RAGManager
│   ├── vector_store.py      # 向量存储（保存/加载）
│   ├── vector_builder.py    # 向量构建（从JSON生成向量）
│   ├── vector_matcher.py    # 向量匹配（查询相似度）
│   └── rag_manager.py       # 统一管理器
├── ragdata/                 # 知识库数据
│   └── rag_knowledge_base_v3.json
├── requirements.txt         # 依赖列表
└── rag_api.md              # 接口文档
```
