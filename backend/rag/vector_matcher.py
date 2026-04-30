import os
import numpy as np
from sentence_transformers import SentenceTransformer, util

from .vector_store import VectorStore

class VectorMatcher:
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        hf_token = os.environ.get("HF_TOKEN")
        if hf_token:
            self.model = SentenceTransformer(model_name, token=hf_token)
        else:
            self.model = SentenceTransformer(model_name)
        self.vector_store = None

    def load_store(self, store_path: str = "./") -> bool:
        """加载向量库"""
        self.vector_store = VectorStore(
            vector_file=f"{store_path}/vectors.npy",
            metadata_file=f"{store_path}/metadata.json"
        )
        return self.vector_store.load()

    def match(self, query: str, top_k: int = 3) -> list:
        """根据查询内容进行匹配，返回最匹配的top_k条结果"""
        if self.vector_store is None or self.vector_store.is_empty():
            raise ValueError("向量库未加载或为空")
        
        query_embedding = self.model.encode(query)
        
        cos_scores = util.cos_sim(query_embedding, self.vector_store.vectors)[0]
        top_results = np.argpartition(-cos_scores, range(top_k))[:top_k]
        
        results = []
        for idx in top_results:
            idx = int(idx)
            results.append({
                'score': float(cos_scores[idx]),
                'filename': self.vector_store.metadata[idx]['filename'],
                'data': self.vector_store.metadata[idx]['raw_data']
            })
        
        results.sort(key=lambda x: x['score'], reverse=True)
        
        return results

    def match_with_threshold(self, query: str, threshold: float = 0.5) -> list:
        """根据查询内容进行匹配，返回相似度超过阈值的结果"""
        if self.vector_store is None or self.vector_store.is_empty():
            raise ValueError("向量库未加载或为空")
        
        query_embedding = self.model.encode(query)
        
        cos_scores = util.cos_sim(query_embedding, self.vector_store.vectors)[0]
        
        results = []
        for idx, score in enumerate(cos_scores):
            if score >= threshold:
                results.append({
                    'score': float(score),
                    'filename': self.vector_store.metadata[idx]['filename'],
                    'data': self.vector_store.metadata[idx]['raw_data']
                })
        
        results.sort(key=lambda x: x['score'], reverse=True)
        
        return results
