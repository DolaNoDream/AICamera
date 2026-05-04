import os
import json
import base64
from pathlib import Path
from typing import List, Dict, Any, Optional, Union
from http import HTTPStatus
from dashscope import Application


class BailianImageRAG:
    """
    阿里云百炼图片RAG检索工具类
    
    用于通过图片和用户需求，检索知识库中匹配的姿势数据。
    """

    def __init__(
        self,
        api_key: Optional[str] = None,
        app_id: Optional[str] = None
    ):
        """
        初始化工具类
        
        Args:
            api_key: DashScope API Key，优先使用环境变量 DASHSCOPE_API_KEY
            app_id: 阿里云百炼应用ID，优先使用环境变量 BAILIAN_APP_ID
        """
        self.api_key = api_key or os.environ.get("DASHSCOPE_API_KEY")
        self.app_id = app_id or os.environ.get("BAILIAN_APP_ID")

        if not self.api_key:
            raise ValueError("必须提供 api_key 或设置环境变量 DASHSCOPE_API_KEY")
        
        if not self.app_id:
            raise ValueError("必须提供 app_id 或设置环境变量 BAILIAN_APP_ID")

    def _encode_image_to_base64(self, image_path_or_bytes: Union[str, bytes]) -> str:
        """
        将图片编码为base64格式的data URL
        
        Args:
            image_path_or_bytes: 图片文件路径或图片二进制数据
        
        Returns:
            base64编码的图片data URL
        """
        if isinstance(image_path_or_bytes, str):
            # 传入的是文件路径
            with open(image_path_or_bytes, 'rb') as f:
                image_data = f.read()
        else:
            # 传入的是二进制数据
            image_data = image_path_or_bytes

        # 检测图片格式
        if image_data.startswith(b'\xff\xd8\xff'):
            mime_type = 'image/jpeg'
        elif image_data.startswith(b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A'):
            mime_type = 'image/png'
        elif image_data.startswith(b'RIFF') and image_data[8:12] == b'WEBP':
            mime_type = 'image/webp'
        else:
            raise ValueError("不支持的图片格式（仅支持JPG/PNG/WEBP）")

        base64_encoded = base64.b64encode(image_data).decode('utf-8')
        return f"data:{mime_type};base64,{base64_encoded}"

    def _parse_response(self, response_text: str) -> List[Dict[str, Any]]:
        """
        解析阿里云应用返回的结果
        
        Args:
            response_text: 应用返回的原始文本
        
        Returns:
            解析后的匹配结果列表
        """
        try:
            results = json.loads(response_text)
        except json.JSONDecodeError:
            # 如果不是有效的JSON，尝试提取content中的JSON
            import re
            json_match = re.search(r'\[.*\]', response_text, re.DOTALL)
            if json_match:
                results = json.loads(json_match.group())
            else:
                return []

        # 处理结果，只保留需要的字段
        parsed_results = []
        for item in results:
            content = item.get('content', '')
            
            # 从content中提取JSON数据
            json_start = content.find('{')
            if json_start != -1:
                try:
                    json_end = content.rfind('}') + 1
                    data_str = content[json_start:json_end]
                    pose_data = json.loads(data_str)
                except (json.JSONDecodeError, ValueError):
                    pose_data = {}
            else:
                pose_data = {}

            parsed_results.append({
                'score': item.get('score', 0.0),
                'filename': item.get('dataId', '') or pose_data.get('filename', ''),
                'scene_context': pose_data.get('scene_context', {}),
                'camera_composition': pose_data.get('camera_composition', {}),
                'pose_overall': pose_data.get('pose_overall', ''),
                'pose_details': pose_data.get('pose_details', {}),
                'facial_expression': pose_data.get('facial_expression', '')
            })

        return parsed_results

    def retrieve(
        self,
        image: Union[str, bytes],
        query: str,
        top_k: int = 5
    ) -> Dict[str, Any]:
        """
        执行图片RAG检索
        
        Args:
            image: 图片文件路径或图片二进制数据
            query: 用户需求字符串（如"请描述这张图片中人物的姿势和动作"）
            top_k: 返回的最匹配结果数量，默认5
        
        Returns:
            包含检索结果的字典，结构如下：
            {
                'success': bool,           # 是否成功
                'message': str,            # 消息
                'results': List[dict],     # 匹配结果列表
                'request_id': str          # 请求ID（如有）
            }
        """
        try:
            # 将图片编码为base64
            image_base64 = self._encode_image_to_base64(image)

            # 调用阿里云应用
            response = Application.call(
                api_key=self.api_key,
                app_id=self.app_id,
                prompt=query,
                images=[image_base64]
            )

            if response.status_code != HTTPStatus.OK:
                return {
                    'success': False,
                    'message': f'调用失败: {response.message}',
                    'results': [],
                    'request_id': response.request_id
                }

            # 解析响应
            results = self._parse_response(response.output.text)
            
            # 按分数排序并取前top_k
            results.sort(key=lambda x: x['score'], reverse=True)
            results = results[:top_k]

            return {
                'success': True,
                'message': '检索成功',
                'results': results,
                'request_id': response.request_id
            }

        except Exception as e:
            return {
                'success': False,
                'message': f'发生错误: {str(e)}',
                'results': [],
                'request_id': None
            }

    def retrieve_best_match(
        self,
        image: Union[str, bytes],
        query: str
    ) -> Dict[str, Any]:
        """
        获取最匹配的一条结果
        
        Args:
            image: 图片文件路径或图片二进制数据
            query: 用户需求字符串
        
        Returns:
            最匹配的结果，如果没有结果则返回None
        """
        result = self.retrieve(image, query, top_k=1)
        if result['success'] and len(result['results']) > 0:
            return result['results'][0]
        return None


# 示例用法
if __name__ == '__main__':
    # 初始化工具类
    rag = BailianImageRAG(
        api_key="sk-40ed2e1b81f545308b6a92574e923706",  # 或设置环境变量
        app_id="7a93bb813d09459c92d96b3108f8a700"      # 或设置环境变量
    )

    script_dir = Path(__file__).resolve().parent
    image_path = script_dir.parent / "image" / "mmexport1752976083598.jpg"
    query = "给我一个活泼的姿势"
    print(image_path)
    
    result = rag.retrieve(image_path, query, top_k=3)
    print("=== 使用图片路径检索 ===")
    print(f"成功: {result['success']}")
    print(f"消息: {result['message']}")
    for i, item in enumerate(result['results']):
        print(f"\n结果 #{i+1} (相似度: {item['score']:.4f})")
        print(f"  图片名: {item['filename']}")
        print(f"  整体姿势: {item['pose_overall']}")
        print(f"  场景环境: {item['scene_context'].get('environment', '')}")
    print("---------------------------------------------------------")
    print(result)

    # 示例2: 使用图片二进制数据
    print("\n" + "="*50)
    print("=== 使用图片二进制数据检索 ===")
    with open(image_path, 'rb') as f:
        image_bytes = f.read()
    
    result2 = rag.retrieve(image_bytes, query, top_k=2)
    print(f"成功: {result2['success']}")
    if result2['success']:
        best = rag.retrieve_best_match(image_bytes, query)
        print(f"最匹配结果 - 相似度: {best['score']:.4f}, 姿势: {best['pose_overall']}")
        print(best)
        print("---------------------------------------------------------")
        print(result2)
