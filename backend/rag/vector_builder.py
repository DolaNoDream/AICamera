import json
import os
from typing import List, Dict, Any
import numpy as np
from sentence_transformers import SentenceTransformer

from .vector_store import VectorStore

class VectorBuilder:
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        hf_token = "hf_token"
        if hf_token:
            self.model = SentenceTransformer(model_name, token=hf_token)
        else:
            self.model = SentenceTransformer(model_name)

    def _extract_text_features(self, item: Dict[str, Any]) -> str:
        """从单个数据项中提取文本特征"""
        extracted_info = item.get('extracted_info', {})
        
        features = []
        
        # 场景上下文
        scene_context = extracted_info.get('scene_context', {})
        if 'environment' in scene_context:
            features.append(f"环境：{scene_context['environment']}")
        if 'interaction_props' in scene_context:
            features.append(f"道具：{scene_context['interaction_props']}")
        if 'vibe_style' in scene_context:
            features.append(f"风格：{scene_context['vibe_style']}")
        
        # 相机构图
        camera_composition = extracted_info.get('camera_composition', {})
        if 'framing' in camera_composition:
            features.append(f"取景：{camera_composition['framing']}")
        if 'angle' in camera_composition:
            features.append(f"角度：{camera_composition['angle']}")
        
        # 整体姿势
        if 'pose_overall' in extracted_info:
            features.append(f"整体姿势：{extracted_info['pose_overall']}")
        
        # 姿势细节
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
        
        vectors = self.model.encode(texts)
        
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
        
        vectors = self.model.encode(texts)
        
        store = VectorStore(
            vector_file=os.path.join(store_path, "vectors.npy"),
            metadata_file=os.path.join(store_path, "metadata.json")
        )
        store.save(vectors, metadata)
        
        print(f"向量库构建完成，共 {len(vectors)} 条数据")
