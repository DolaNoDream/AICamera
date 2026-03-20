import mimetypes
import base64
from typing import Optional, Dict, Any
import os
import dashscope
from dashscope import MultiModalConversation

dashscope.api_key = os.getenv("DASHSCOPE_API_KEY")

if not dashscope.api_key:
    raise ValueError("DASHSCOPE_API_KEY 未配置")
def encode_image_bytes(image_bytes: bytes, filename: str = "image.jpg") -> str:
    mime_type, _ = mimetypes.guess_type(filename)
    if not mime_type or not mime_type.startswith("image/"):
        mime_type = "image/jpeg"
    encoded = base64.b64encode(image_bytes).decode('utf-8')
    return f"data:{mime_type};base64,{encoded}"


def build_edit_prompt(requirement: Dict[str, Any]) -> str:
    filter_str = requirement.get("filter") or "保持原风格"
    portrait_str = requirement.get("portrait") or "无调整"
    background_str = requirement.get("background") or "保持原背景"
    special_str = requirement.get("special") or "无特殊要求"

    prompt = f"""
你是一个专业、高精度、听话的AI图片编辑大师。请严格按照用户以下**每一条明确要求**，对输入图片进行精确编辑，不要添加任何未提及的内容。

【滤镜风格要求】：{filter_str}
【人像调整要求】：{portrait_str}
【背景处理要求】：{background_str}
【特殊修改要求】：{special_str}

=== 必须严格遵守的核心编辑原则（最高优先级）===
1. 人物核心特征（面部五官、身材比例、姿势、发型、光影、肤色）必须尽量保持不变，除非“特殊修改要求”中明确提到要改。
2. 所有编辑必须自然融合、无拼接痕迹、无模糊、无光影不一致。
3. 禁止添加任何用户未提及的元素（额外人物、文字、水印、道具、特效）。
4. 如果要求包含“去人群”“瘦脸”“换背景”等常见操作，请智能、精准实现，不要过度变形或毁图。
5. 输出必须是高质量、清晰、自然的完整图片（PNG格式最佳）。

现在请直接生成编辑后的图片，不要解释，不要额外文字。
""".strip()

    return prompt


def generate_edited_image_url(
    image_bytes: bytes,
    requirement: Dict[str, Any],
    filename: str = "input.jpg",
    api_key: Optional[str] = None,
) -> str:
    """
    主函数（保持不变，仅提示词升级）
    """
    if api_key:
        dashscope.api_key = api_key

    if not dashscope.api_key:
        raise ValueError("DashScope API Key 未配置")

    image_data_uri = encode_image_bytes(image_bytes, filename)
    edit_prompt = build_edit_prompt(requirement)

    messages = [
        {
            "role": "user",
            "content": [
                {"image": image_data_uri},
                {"text": edit_prompt}
            ]
        }
    ]

    try:
        response = MultiModalConversation.call(
            model="qwen-image-edit-plus",          # 如有新模型可替换为 qwen-image-edit-最新版
            messages=messages,
            stream=False,
            n=1,
            watermark=False,
            negative_prompt="模糊,畸形,低质量,拼接痕迹,过度变形,多余人物,文字,水印,毁脸,光影不一致",
            prompt_extend=True,
            temperature=0.6,                       # 略微降低创造性，保证听话
        )

        if response.status_code != 200:
            raise Exception(
                f"API 调用失败 - HTTP {response.status_code}, "
                f"code: {getattr(response, 'code', 'N/A')}, "
                f"msg: {getattr(response, 'message', str(response))}"
            )

        content = response.output.choices[0].message.content
        if isinstance(content, list) and len(content) > 0 and "image" in content[0]:
            return content[0]["image"]

        raise Exception("响应中未找到图片 URL")

    except Exception as e:
        raise Exception(f"AI p图失败：{str(e)}")