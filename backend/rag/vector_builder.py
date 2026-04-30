import json
import os
from typing import List, Dict, Any, Optional
import numpy as np

from .vector_store import VectorStore


class VectorBuilder:
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

    def _extract_text_features(self, item: Dict[str, Any]) -> str:
        """从单个数据项中提取文本特征"""
        extracted_info = item.get('extracted_info', {})
        
        features = []
        
        scene_context = extracted_info.get('scene_context', {})
        if 'environment' in scene_context:
            features.append(f"环境：{scene_context['environment']}")
        if 'interaction_props' in scene_context:
            features.append(f"道具：{scene_context['interaction_props']}")
        if 'vibe_style' in scene_context:
            features.append(f"风格：{scene_context['vibe_style']}")
        
        camera_composition = extracted_info.get('camera_composition', {})
        if 'framing' in camera_composition:
            features.append(f"取景：{camera_composition['framing']}")
        if 'angle' in camera_composition:
            features.append(f"角度：{camera_composition['angle']}")
        
        if 'pose_overall' in extracted_info:
            features.append(f"整体姿势：{extracted_info['pose_overall']}")
        
        pose_details = extracted_info.get('pose_details', {})
        if 'head' in pose_details:
            features.append(f"头部：{pose_details['head']}")
        if 'arms' in pose_details:
            features.append(f"手臂：{pose_details['arms']}")
        if 'hands' in pose_details:
            features.append(f"手部：{pose_details['hands']}")
        if 'torso' in pose_details:
            features.append(f"躯干：{pose_details['torso']}")
        if 'legs' in pose_details:
            features.append(f"腿部：{pose_details['legs']}")
        if 'orientation' in pose_details:
            features.append(f"朝向：{pose_details['orientation']}")
        
        return ' '.join(features)

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

    def _encode(self, texts: List[str]) -> np.ndarray:
        """根据使用的框架进行文本编码"""
        if self.use_modelscope:
            embeddings = []
            for text in texts:
                result = self.model(text)
                embedding = self._get_embedding_from_result(result)
                embeddings.append(embedding)
            
            if len(embeddings) == 0:
                return np.array([])
            
            return np.array(embeddings)
        else:
            return self.model.encode(texts)

    def build_from_json(self, json_path: str, store_path: str = "./") -> None:
        """从JSON文件构建向量库"""
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        texts = []
        metadata = []
        
        for item in data:
            text = self._extract_text_features(item)
            texts.append(text)
            metadata.append({
                'filename': item.get('filename', ''),
                'raw_data': item
            })
        
        vectors = self._encode(texts)
        
        store = VectorStore(
            vector_file=os.path.join(store_path, "vectors.npy"),
            metadata_file=os.path.join(store_path, "metadata.json")
        )
        store.save(vectors, metadata)
        
        print(f"向量库构建完成，共 {len(vectors)} 条数据")

    def build_from_data(self, data: List[Dict[str, Any]], store_path: str = "./") -> None:
        """从数据列表构建向量库"""
        texts = []
        metadata = []
        
        for item in data:
            text = self._extract_text_features(item)
            texts.append(text)
            metadata.append({
                'filename': item.get('filename', ''),
                'raw_data': item
            })
        
        vectors = self._encode(texts)
        
        store = VectorStore(
            vector_file=os.path.join(store_path, "vectors.npy"),
            metadata_file=os.path.join(store_path, "metadata.json")
        )
        store.save(vectors, metadata)
        
        print(f"向量库构建完成，共 {len(vectors)} 条数据")
