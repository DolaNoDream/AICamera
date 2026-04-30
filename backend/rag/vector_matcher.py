import os
import numpy as np
from scipy.spatial.distance import cosine

from .vector_store import VectorStore


class VectorMatcher:
    def __init__(self, model_name: str = "all-MiniLM-L6-v2", use_modelscope: bool = False, model=None):
        self.use_modelscope = use_modelscope
        self.model_name = model_name
        
        if model is not None:
            self.model = model
        elif use_modelscope:
            from modelscope.pipelines import pipeline
            self.model = pipeline("feature-extraction", model=model_name)
        else:
            from sentence_transformers import SentenceTransformer
            hf_token = os.environ.get("HF_TOKEN")
            if hf_token:
                self.model = SentenceTransformer(model_name, token=hf_token)
            else:
                self.model = SentenceTransformer(model_name)
        
        self.vector_store = None

    def _mean_pooling(self, token_embeddings: np.ndarray) -> np.ndarray:
        """对token嵌入进行均值池化，得到句子嵌入"""
        return np.mean(token_embeddings, axis=0)

    def _get_embedding_from_result(self, result) -> np.ndarray:
        """从ModelScope结果中提取嵌入向量并进行池化"""
        if isinstance(result, dict):
            if 'output' in result:
                output = result['output']
                if isinstance(output, list):
                    if len(output) > 0:
                        if isinstance(output[0], list):
                            return self._mean_pooling(np.array(output[0]))
                        return self._mean_pooling(np.array(output))
                return self._mean_pooling(np.array(output))
            elif 'logits' in result:
                logits = result['logits']
                if isinstance(logits, list):
                    if len(logits) > 0 and isinstance(logits[0], list):
                        return self._mean_pooling(np.array(logits[0]))
                return self._mean_pooling(np.array(logits))
        
        if isinstance(result, list):
            if len(result) > 0:
                if isinstance(result[0], list):
                    return self._mean_pooling(np.array(result))
                return np.array(result)
        
        if isinstance(result, np.ndarray):
            if len(result.shape) == 2:
                return self._mean_pooling(result)
            return result
        
        return np.array(result)

    def _encode(self, text: str) -> np.ndarray:
        """根据使用的框架进行文本编码"""
        if self.use_modelscope:
            result = self.model(text)
            return self._get_embedding_from_result(result)
        else:
            return self.model.encode(text)

    def load_store(self, store_path: str = "./") -> bool:
        """加载向量库"""
        self.vector_store = VectorStore(
            vector_file=os.path.join(store_path, "vectors.npy"),
            metadata_file=os.path.join(store_path, "metadata.json")
        )
        return self.vector_store.load()

    def match(self, query: str, top_k: int = 3) -> list:
        """根据查询内容进行匹配，返回最匹配的top_k条结果"""
        if self.vector_store is None or self.vector_store.is_empty():
            raise ValueError("向量库未加载或为空")
        
        query_embedding = self._encode(query)
        
        cos_scores = []
        for vec in self.vector_store.vectors:
            similarity = 1 - cosine(query_embedding, vec)
            cos_scores.append(similarity)
        
        cos_scores = np.array(cos_scores)
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
        
        query_embedding = self._encode(query)
        
        cos_scores = []
        for vec in self.vector_store.vectors:
            similarity = 1 - cosine(query_embedding, vec)
            cos_scores.append(similarity)
        
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
