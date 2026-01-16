# GeneratePose.py 完整修正版
import base64
import json
import random
from typing import Optional, Dict


def encode_image_bytes(image_data: bytes) -> str:
    """将图片二进制数据编码为base64格式的data URL"""

    def get_image_mime_type(img_data: bytes) -> str:
        if img_data.startswith(b'\xff\xd8\xff'):
            return 'image/jpeg'
        elif img_data.startswith(b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A'):
            return 'image/png'
        elif img_data.startswith(b'RIFF') and img_data[8:12] == b'WEBP':
            return 'image/webp'
        else:
            raise ValueError("不支持的图片格式（仅支持JPG/PNG/WEBP）")

    mime_type = get_image_mime_type(image_data)
    base64_encoded = base64.b64encode(image_data).decode('utf-8')
    return f"data:{mime_type};base64,{base64_encoded}"


def generate_pose_suggestions(
        image_data: bytes,
        user_intent: Optional[str] = None,
        meta: Optional[Dict] = None,
        random_seed: Optional[int] = None
) -> str:
    """
    调用通义千问VL大模型生成精细化姿势建议+构图指导+语音指导
    确保返回值是标准JSON字符串
    """
    # 核心提示词（保持你之前的版本不变）
    user_intent_str = user_intent if user_intent else "无"
    meta_str = json.dumps(meta, ensure_ascii=False) if meta else "无"

    forbidden_rules = """
=== 严格禁止项（必须遵守，否则输出无效） ===
1. 所有字段中绝对禁止出现任何表情相关描述：
   - 禁止词汇：微笑、大笑、撇嘴、皱眉、眨眼、表情、神态、情绪、五官等；
   - 禁止示例："头部微侧微笑" → 正确示例："头部微微向右侧倾"。
2. 所有字段中绝对禁止出现任何衣着/装饰/鞋袜相关描述：
   - 禁止词汇：衣服、裤子、鞋子、帽子、配饰、穿搭、短袖、裙子、赤裸、赤脚等；
   - 禁止示例："赤脚站立" → 正确示例："双脚平行站立，脚掌完全贴地"。
    """.strip()

    diversity_rules = """
=== 多样性要求 ===
1. 每次生成必须选择不同的姿势组合，避免重复相同的姿势名称和动作描述；

2. 保持核心拍摄意图不变，但具体姿势和操作提示需有明显差异。
    """.strip()

    prompt = f"""
任务：根据上传的图片场景、用户输入要求、相机参数，生成符合要求的姿势建议+构图指导+语音指导（用于AI相机场景，需实用、易操作且贴合场景）。

{forbidden_rules}

{diversity_rules}

=== 格式要求 ===
1. 输出格式：严格遵循以下JSON结构，字段不可缺失、不可新增，值为中文且简洁易懂：
{{
  "poseSuggestions": [
    {{
      "id": "p001",
      "name": "姿势名称"（4-8字，仅描述肢体姿态，无表情/衣着）,
      "priority": 数字（1为最高优先级，依次递增，最多返回3个姿势）,
      "details": {{
        "head": "头部姿态描述（无表情）",
        "arms": "手臂姿态描述",
        "hands": "手部姿态描述",
        "torso": "躯干姿态描述",
        "hips": "髋部姿态描述（可选）",
        "legs": "腿部姿态描述",
        "feet": "脚部姿态描述（可选）",
        "orientation": "整体朝向描述"
      }},
      "tips": ["操作提示1", "操作提示2", "操作提示3"]（每个提示5-10字，仅描述肢体动作，可直接语音播报）
    }}
  ],
  "compositionGuide": {{
    "framing": "取景建议",
    "angle": "拍摄角度建议",
    "background": "背景处理建议",
    "symmetry": "构图对称/平衡建议"
  }},
  "voiceGuide": "语音引导文本"（50-80字，口语化、亲切，整合姿势和构图核心建议，仅描述肢体动作和构图调整）
}}

2. 生成逻辑：
- 姿势建议：优先分析图片中的人物肢体姿态，指出不足并给出优化建议，按身体部位精细化描述，给出的姿势描述不能相互矛盾且要清晰（例如，如果有侧身，要指明向哪个方向侧身）；
- 构图指导：结合图片场景给出取景、角度、背景、对称等维度的实用建议；
- 语音指导：整合姿势和构图核心建议，口语化表达，适合语音播报；
- 所有描述仅关注肢体姿态和构图，无表情/衣着/装饰相关内容；
- tips需具体可执行，不使用专业术语；
- details字段中可选的（hips/feet）可根据姿势合理性决定是否保留。

输入信息：
- 用户拍摄意图：{user_intent_str}
- 相机参数：{meta_str}

输出要求：仅返回JSON字符串，无任何额外文字、注释或格式说明，确保可直接解析。
    """.strip()

    # 调用大模型函数
    def call_llm(prompt: str) -> str:
        from openai import OpenAI
        client = OpenAI(
            api_key=
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
        )

        image_data_url = encode_image_bytes(image_data)
        seed = random.randint(1, 1000000) if random_seed is None else random_seed

        response = client.chat.completions.create(
            model="qwen3-vl-plus",
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": image_data_url}},
                        {"type": "text", "text": prompt},
                    ],
                }
            ],
            temperature=0.95,
            seed=seed,
            timeout=30
        )
        return response.choices[0].message.content.strip()

    # 调用模型并处理响应
    try:
        llm_raw_response = call_llm(prompt)

        # 解析并标准化响应
        llm_result = json.loads(llm_raw_response)
        sorted_poses = sorted(llm_result["poseSuggestions"], key=lambda x: x["priority"])
        for idx, pose in enumerate(sorted_poses):
            pose["id"] = f"p{idx + 1:03d}"
        llm_result["poseSuggestions"] = sorted_poses

        # 关键：确保返回的是JSON字符串（而非字典/列表）
        final_json = json.dumps(llm_result, ensure_ascii=False, indent=2)

        # 最终校验：返回值必须是字符串
        if not isinstance(final_json, str):
            raise RuntimeError("生成的姿势建议不是字符串类型")

        return final_json

    except json.JSONDecodeError as e:
        raise Exception(f"解析LLM响应失败：{str(e)}，原始响应：{llm_raw_response}")
    except KeyError as e:
        raise Exception(f"LLM响应缺少字段：{str(e)}，原始响应：{llm_raw_response}")
    except Exception as e:
        raise Exception(f"生成姿势建议失败：{str(e)}")