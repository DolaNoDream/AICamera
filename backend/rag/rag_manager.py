import os
from typing import List, Dict, Any, Optional


class RAGManager:
    def __init__(self, store_path: str = "./vector_store", use_modelscope: bool = False, model_name: str = "all-MiniLM-L6-v2"):
        self.store_path = store_path
        self.use_modelscope = use_modelscope
        self.model_name = model_name
        
        # 先加载共享模型
        self._load_model()
        
        # 导入并创建builder和matcher，共享同一个模型
        from .vector_builder import VectorBuilder
        from .vector_matcher import VectorMatcher
        
        self.builder = VectorBuilder(model_name=model_name, use_modelscope=use_modelscope, model=self.model)
        self.matcher = VectorMatcher(model_name=model_name, use_modelscope=use_modelscope, model=self.model)
        
        self._ensure_store_path()

    def _load_model(self):
        """加载共享的嵌入模型"""
        if self.use_modelscope:
            from modelscope.pipelines import pipeline
            self.model = pipeline("feature-extraction", model=self.model_name)
        else:
            from sentence_transformers import SentenceTransformer
            hf_token = os.environ.get("HF_TOKEN")
            if hf_token:
                self.model = SentenceTransformer(self.model_name, token=hf_token)
            else:
                self.model = SentenceTransformer(self.model_name)

    def _ensure_store_path(self) -> None:
        """确保向量库存储路径存在"""
        if not os.path.exists(self.store_path):
            os.makedirs(self.store_path)

    def build_vector_store(self, json_path: Optional[str] = None, data: Optional[List[Dict[str, Any]]] = None) -> None:
        """
        构建向量库
        
        Args:
            json_path: JSON文件路径
            data: 数据列表（与json_path二选一）
        """
        if json_path:
            self.builder.build_from_json(json_path, self.store_path)
        elif data:
            self.builder.build_from_data(data, self.store_path)
        else:
            raise ValueError("必须提供json_path或data参数")
        
        # 构建完成后自动加载向量库
        self.load_vector_store()

    def load_vector_store(self) -> bool:
        """加载向量库"""
        return self.matcher.load_store(self.store_path)

    def match(self, query: str, top_k: int = 3) -> list:
        """
        根据查询内容进行匹配
        
        Args:
            query: 查询文本
            top_k: 返回最匹配的条数
        
        Returns:
            匹配结果列表，包含score、filename和data字段
        """
        return self.matcher.match(query, top_k)

    def match_with_threshold(self, query: str, threshold: float = 0.5) -> list:
        """
        根据查询内容进行匹配，返回相似度超过阈值的结果
        
        Args:
            query: 查询文本
            threshold: 相似度阈值
        
        Returns:
            匹配结果列表，包含score、filename和data字段
        """
        return self.matcher.match_with_threshold(query, threshold)

    def get_best_match(self, query: str) -> Optional[dict]:
        """
        获取最匹配的一条结果
        
        Args:
            query: 查询文本
        
        Returns:
            最匹配的结果，包含score、filename和data字段，若无匹配则返回None
        """
        results = self.matcher.match(query, top_k=1)
        return results[0] if results else None
