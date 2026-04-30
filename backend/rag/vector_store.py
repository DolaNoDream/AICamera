import json
import os
from typing import List, Dict, Any
import numpy as np

class VectorStore:
    def __init__(self, vector_file: str = "vectors.npy", metadata_file: str = "metadata.json"):
        self.vector_file = vector_file
        self.metadata_file = metadata_file
        self.vectors = None
        self.metadata = None

    def save(self, vectors: np.ndarray, metadata: List[Dict[str, Any]]) -> None:
        np.save(self.vector_file, vectors)
        with open(self.metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, ensure_ascii=False, indent=2)

    def load(self) -> bool:
        if os.path.exists(self.vector_file) and os.path.exists(self.metadata_file):
            self.vectors = np.load(self.vector_file)
            with open(self.metadata_file, 'r', encoding='utf-8') as f:
                self.metadata = json.load(f)
            return True
        return False

    def is_empty(self) -> bool:
        return self.vectors is None or len(self.vectors) == 0
