# GenerateWrite.py
import json
from typing import List, Dict
import base64
import os
from openai import OpenAI

def encode_image_bytes(image_data: bytes) -> str:
    """将图片二进制数据编码为base64格式的data URL"""
    if image_data.startswith(b'\xff\xd8\xff'):
        mime_type = 'image/jpeg'
    elif image_data.startswith(b'\x89\x50\x4E\x47\x0D\x0A\x1A\x0A'):
        mime_type = 'image/png'
    elif image_data.startswith(b'RIFF') and image_data[8:12] == b'WEBP':
        mime_type = 'image/webp'
    else:
        mime_type = 'image/jpeg'

    base64_encoded = base64.b64encode(image_data).decode('utf-8')
    return f"data:{mime_type};base64,{base64_encoded}"

def generate_ai_write(image_data_list: List[bytes], requirement: Dict) -> List[str]:
    """
    接收多张图片二进制数据和文案要求字典，调用大模型生成文案
    返回包含3条不同的文案列表
    """

    # API Key
    client = OpenAI(
        api_key=os.getenv("DASHSCOPE_API_KEY"),
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
    )
    if not os.getenv("DASHSCOPE_API_KEY"):
        raise ValueError("DASHSCOPE_API_KEY 未配置")

    # 1. 组装多模态内容列表（直接使用前端传来的图片URL）
    content_list = []
    for img_bytes in image_data_list:
        img_url = encode_image_bytes(img_bytes)
        content_list.append({"type": "image_url", "image_url": {"url": img_url}})

    # 2. 动态解析并翻译 requirement 字典，构建精准 Prompt
    req_lines = []
    if requirement:
        req_mapping = {
            "type": "文案发布平台/类型",
            "location": "拍摄地点",
            "weather": "拍摄时的天气",
            "emotion": "基调情感",
            "theme": "核心主题",
            "style": "文字风格",
            "length": "篇幅要求",
            "special": "特殊情境/要求",
            "custom": "用户的额外自定义叮嘱（最高优先级）"
        }
        for key, label in req_mapping.items():
            if key in requirement and requirement[key]:
                req_lines.append(f"- {label}：{requirement[key]}")

    req_str = "\n".join(req_lines) if req_lines else "- 无特殊限制，请根据图片自由发挥最贴合的网感文案。"

    # 3. 核心大招：平台专属语境规则注入 (Platform-Specific Prompt Engineering)
    platform_rules = ""
    platform_type = requirement.get("type", "") if requirement else ""

    if platform_type == "朋友圈":
        platform_rules = """
    【朋友圈专属规则】：
    1. 语感要求：真诚、自然、有私人生活气息，但不要像对话，绝对拒绝营销号口吻。
    2. 格式要求：短小精悍，不要啰嗦。可以带少许日常Emoji。
    3. 禁忌：不要带任何 #Hashtag 标签，不要分太多段落。
    """
    elif platform_type == "小红书":
        platform_rules = """
    【小红书专属规则】：
    1. 标题要求：第一行必须是吸引眼球的网感标题，建议用【】或Emoji包裹强调。
    2. 语感要求：热情、有分享欲（“种草”感）、闺蜜语气，多从画面中挖掘穿搭、风景、情绪亮点，尽量以第一人称视角。
    3. 排版要求：必须分段落布局，段落之间要多穿插契合语境的Emoji表情，提升视觉丰富度。
    4. 标签要求：文末必须带上 4-6 个紧贴画面的高流量标签（如 #周末去哪儿 #OOTD #我的私藏风景）。
    """
    elif platform_type == "抖音":
        platform_rules = """
    【抖音专属规则】：
    1. 语感要求：短平快，具有极强的情绪感染力或悬念感，文案旨在引导用户在评论区留言互动。
    2. 格式要求：一到两句话即可，尽量不带Emoji。
    3. 标签要求：文末必须附带 3-5 个热门标签#标签（Hashtag）（如 #日常碎片 #美好生活）（不算在字数要求内）。
    """
    elif platform_type.lower() in ["instagram", "ins"]:
        platform_rules = """
    【Instagram专属规则】：
    1. 语感要求：极简、高级、清冷或松弛感，注重视觉美学。
    2. 格式要求：极简英文短句，或中英双语。
    3. Emoji要求：使用有质感、低饱和度的Emoji（如 ☕️✨🤍🕊️🌿🌊），拒绝土味表情。
    4. 标签要求：文末空一行，附带一排（5-8个）精准的英文 Hashtag。
    """
    # 4. 组装终极 Prompt
    length_req = requirement.get("length", "适中（字数由平台常规习惯决定）") if requirement else "适中"
    prompt = f"""
你是一个精通全网各大社交媒体算法和年轻一代心理学的资深文案大师。
请仔细观察我上传的图片（可能有一张或多张）。结合画面中的穿搭、构图、光影细节，以及以下用户的具体要求，为我生成3条可以直接复制发布的完美文案。

【具体要求】
{req_str}

{platform_rules}

=== 多样性与一致性要求（极其重要） ===
1. **风格一致性**：这 3 条文案必须**严格遵循**用户在“文字风格”和“基调情感”中指定的要求，绝不能偏离设定的核心氛围。
2. **切入点多样性**：在保持风格一致的前提下，请使用 **完全不同的切入角度或表达方式**：
   - 备选 1：侧重于客观环境描写（如光影、构图、天气）与心境的交融。
   - 备选 2：侧重于主观情感抒发、人生感悟或状态表达。
   - 备选 3：侧重于与画面细节（如穿搭、某个小动作、背景路人等）的趣味互动或微观刻画
3. **【最高防偷懒警告 / 篇幅强制约束】**：用户明确要求的正文篇幅是：【{length_req}】。
   **这 3 条备选文案的每一条，都必须独立、完整地填满这个篇幅要求！** 
    **特殊豁免规则**：如果当前平台的专属规则中要求添加 `#标签（Hashtag）`，请务必在文末加上！**这些标签不计入字数限制**。千万不要因为字数限制而把平台要求的标签吞掉！
   **绝不允许因为一次性生成 3 条，就大幅缩减单条的字数或偷工减料！如果用户要求长文、多段落或几百字，请确保这 3 条每一条都是内容极度饱满的长文！**   
【输出严格规范】
- 你的身份是直接帮我写好文案的助理，请**仅输出最终的文案内容**。
- 数组中必须且只能包含 3 个字符串元素，分别对应 3 条不同的备选文案。
- **绝对不要**包含任何诸如“好的，为你生成”、“这是为您准备的小红书文案：”等废话前言。
- **绝对不要**使用 Markdown 的代码块符号（如 ```json 或 ```）。直接输出纯文本排版。
    
期望的纯净 JSON 输出格式示例：
[
    "第一条文案内容（可换行）",
    "第二条文案内容（可换行）",
    "第三条文案内容（可换行）"
]    
    
    """.strip()

    content_list.append({"type": "text", "text": prompt})

    # 3. 发起请求
    try:
        response = client.chat.completions.create(
            model="qwen3-vl-plus",  # 使用通义千问VL模型
            messages=[{"role": "user", "content": content_list}],
            temperature=0.85,  # 稍高温度，增加文案创意性
            timeout=40
        )

        raw_text = response.choices[0].message.content.strip()
        # 清除可能残留的markdown符号
        if raw_text.startswith("```"):
            raw_text = raw_text.split("\n", 1)[-1]
        if raw_text.endswith( "```"):
            raw_text = raw_text.rsplit("\n", 1)[0]


        # 尝试解析 JSON 数组
        try:
            result_list = json.loads(raw_text)
            if isinstance(result_list, list) and len(result_list) > 0:
                # 确保最多只返回3条，防止模型发神经返回太多
                return result_list[:3]
            else:
                # 兜底：如果模型返回的虽然是JSON但不是列表
                return [raw_text]
        except json.JSONDecodeError:
            # 终极兜底：如果模型没有听话返回JSON格式，把它按普通换行或数字序号切分
            import re
            fallback_list = re.split(r'\n(?=\d+\.|方案[一二三]|文案[123])', raw_text)
            if len(fallback_list) > 1:
                return [item.strip() for item in fallback_list if item.strip()][:3]
            return [raw_text]  # 如果切不开，就把整段作为唯一一个元素返回

    except Exception as e:
        raise Exception(f"模型生成失败：{str(e)}")