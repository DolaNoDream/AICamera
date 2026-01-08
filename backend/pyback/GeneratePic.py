import mimetypes
import json
import os
from typing import Optional, Dict, Any
import dashscope
from dashscope import MultiModalConversation
import base64
import requests

# ======================== 全局配置 ========================
# 地域配置：北京地域（新加坡地域替换为 https://dashscope-intl.aliyuncs.com/api/v1）
dashscope.base_http_api_url = 'https://dashscope.aliyuncs.com/api/v1'

# API Key（替换为你的真实Key，北京/新加坡地域Key不同）
API_KEY = "sk-8eb3cf8f28ab454ba7c84819e7cac905"


# ======================== 工具函数 ========================
def encode_file(file_path):
    mime_type, _ = mimetypes.guess_type(file_path)
    if not mime_type or not mime_type.startswith("image/"):
        raise ValueError("不支持或无法识别的图像格式")
    try:
        with open(file_path, "rb") as image_file:
            encoded_string = base64.b64encode(
                image_file.read()).decode('utf-8')
        return f"data:{mime_type};base64,{encoded_string}"
    except IOError as e:
        raise IOError(f"读取文件时出错: {file_path}, 错误: {str(e)}")


def get_highest_priority_pose(pose_json_str: str) -> Dict[str, Any]:
    """ 从姿势建议JSON中提取优先级最高的姿势
    :param pose_json_str: 姿势建议JSON字符串
    :return: 优先级最高的姿势字典
    """
    try:
        pose_data = json.loads(pose_json_str)
        # 按priority升序排序，取第一个（优先级最高）
        sorted_poses = sorted(pose_data["poseSuggestions"], key=lambda x: x["priority"])
        if not sorted_poses:
            raise ValueError("姿势建议列表为空")
        return sorted_poses[0]
    except json.JSONDecodeError as e:
        raise Exception(f"解析姿势JSON失败：{str(e)}")
    except KeyError as e:
        raise Exception(f"姿势JSON缺少必要字段：{str(e)}")


def generate_detailed_pose_description_cn(pose: Dict[str, Any]) -> str:
    """ 将姿势字典转换为非常详细的中文部位描述（新增：每个部位单独从details中提取并组合）
    :param pose: 优先级最高的姿势字典（必须包含 "details" 字段）
    :return: 详细的中文姿势描述字符串（结构化、按部位分段）
    """
    lines = []

    # 整体姿势名称（可选）
    if "name" in pose:
        lines.append(f"整体姿势名称：{pose['name']}")

    # 必须包含 details 字典，按固定顺序提取常见部位（可扩展）
    details = pose.get("details", {})
    part_mapping = {
        "head": "头部",
        "face": "面部/目光",  # 可选
        "arms": "双臂",
        "hands": "双手",
        "torso": "躯干/上身",
        "hips": "髋部",  # 可选
        "legs": "腿部",
        "feet": "双脚",
        "orientation": "整体朝向/视角"  # 可选，用于指定正视、侧身等
    }

    for key, label in part_mapping.items():
        if key in details:
            lines.append(f"{label}：{details[key]}")

    # 额外tips（如果存在）
    if "tips" in pose and pose["tips"]:
        tips_str = "；".join(pose["tips"])
        lines.append(f"额外动作提示：{tips_str}")

    return "\n".join(lines)


# ======================== 核心生成函数 ========================
def generate_pose_image_url(
        pose_json_str: str,
        reference_image_url: str = "https://i.cetsteam.com/imgs/2026/01/05/73211dce4af89615.jpeg",
        api_key: Optional[str] = None
) -> str:
    """ 根据姿势建议JSON生成优先级最高的姿势示意图URL（仅返回URL，不下载）
    :param pose_json_str: 姿势建议JSON字符串
    :param reference_image_url: 参考图片URL（基础线条风格）
    :param api_key: 自定义API Key（默认使用全局配置）
    :return: 生成的姿势示意图URL
    """
    # 1. 校验参数
    if not api_key:
        api_key = API_KEY
    if not api_key:
        raise ValueError("API Key不能为空，请配置全局API_KEY或传入参数")

    # 2. 提取优先级最高的姿势并生成详细中文描述
    highest_pose = get_highest_priority_pose(pose_json_str)
    pose_desc_cn = generate_detailed_pose_description_cn(highest_pose)
    print(f"正在生成姿势示意图（详细描述）:\n{pose_desc_cn}")

    # 替换为本地参考图的base64编码（确保文件路径正确）
    reference_image_url = encode_file("D:\\学习资料\\软创\\example.png")

    # 3. 重构后的细化提示词（更详细、结构化，按部位引导模型）
    prompt = f"""
仅参考提供的参考图片的风格（黑色细线、极简火柴人、线框样式），完全忽略参考图中的具体人物姿势，严格按照以下详细部位描述生成全新的极简火柴人姿势示意图：

{pose_desc_cn}

关键要求：
1. 风格统一：仅使用黑色均匀细线条勾勒身体轮廓和主要关节，极简火柴人风格，无任何面部特征、头发、衣物、装饰或多余细节；
2. 视角：全身视图，优先采用正视图，除非“整体朝向/视角”中明确指定侧身、背面或其他角度；
3. 比例与平衡：人体比例自然，重心分布符合描述，姿势稳定自然；
4. 输出格式：PNG格式，背景完全透明（带alpha通道），高分辨率，边缘锐利无抗锯齿模糊；
5. 严格禁止：彩色元素、阴影、渐变、背景内容、文字水印、多余肢体、模糊、低质量、与描述不符的姿势。
""".strip()

    # 4. 构造模型请求消息（纯中文提示词）
    messages = [
        {
            "role": "user",
            "content": [
                {"image": reference_image_url},
                {"text": prompt}
            ]
        }
    ]

    # 5. 调用通义千问图片编辑模型
    try:
        response = MultiModalConversation.call(
            api_key=api_key,
            model="qwen-image-edit-plus",
            messages=messages,
            stream=False,
            n=1,  # 仅生成1张图
            watermark=False,
            negative_prompt="低质量,模糊,彩色,多余细节,面部特征,衣物,背景有颜色,文字,阴影,渐变",
            prompt_extend=True
        )

        # 6. 解析响应，返回图片URL
        if response.status_code == 200:
            image_content = response.output.choices[0].message.content[0]
            image_url = image_content["image"]
            return image_url
        else:
            raise Exception(
                f"模型调用失败 - HTTP码：{response.status_code}, "
                f"错误码：{response.code}, 错误信息：{response.message}"
            )
    except Exception as e:
        raise Exception(f"生成姿势示意图失败：{str(e)}")


# ======================== 测试用例 ========================
if __name__ == "__main__":
    # 重构后的示例姿势建议JSON（每个pose新增 "details" 字段，按部位详细描述）
    sample_pose_json = """
    {
      "poseSuggestions": [
        {
          "id": "p001",
          "name": "正面抬手侧身",
          "priority": 2,
          "details": {
            "head": "头部略微向右侧倾，目光直视镜头方向，下巴微收",
            "face": "无面部表情，仅简单圆头，无五官",
            "arms": "右手自然弯曲抬起，手肘约90度，手掌置于额头旁做遮阳姿势；左手自然下垂贴近身体",
            "hands": "双手手指自然并拢，无夸张手势",
            "torso": "躯干向右侧微转约45度，肩膀放松，一肩略高于另一肩",
            "hips": "髋部轻微侧移，重心偏向左腿",
            "legs": "左腿笔直承重，右腿微微弯曲放松向后微移，形成自然站姿",
            "feet": "双脚平行站立，脚尖略微外八，左脚完全着地，右脚跟轻微抬起",
            "orientation": "整体微侧身（约45度）面向镜头，全身正视图为主"
          },
          "tips": [
            "身体微侧45度",
            "单手自然抬起遮阳",
            "重心前脚支撑，显腿长"
          ]
        },
        {
          "id": "p002",
          "name": "叉腰前倾站立",
          "priority": 3,
          "details": {
            "head": "头部正对镜头，微微前倾，下巴回收",
            "arms": "双手弯曲置于腰部两侧，手肘向后张开",
            "hands": "双手叉在髋部，手掌向内",
            "torso": "上身微微前倾约15-20度，胸部挺起",
            "legs": "双腿分开与肩同宽，膝盖微锁",
            "orientation": "完全正面朝向镜头"
          },
          "tips": [
            "双手叉腰挺胸",
            "上身微微前倾",
            "双脚稳立地面"
          ]
        },
        {
          "id": "p003",
          "name": "踮脚单侧抬臂",
          "priority": 1,
          "details": {
            "head": "头部正对镜头，略微上扬",
            "arms": "右侧手臂向上伸直举过头顶，左侧手臂自然下垂",
            "torso": "躯干略向左侧倾斜以保持平衡",
            "legs": "左脚完全着地承重，右脚踮起脚尖离地",
            "feet": "左脚平放，右脚仅脚尖触地",
            "orientation": "正面朝向镜头"
          },
          "tips": [
            "单脚轻踮离地",
            "一侧手臂上扬",
            "重心偏移侧方"
          ]
        }
      ],
      "overlayText": "推荐姿势：侧身抬手（显腿长）",
      "voiceGuide": "你可以试试侧身30°朝向镜头，右手抬到额头旁，肩膀放松，这样拍出来显腿长哦～"
    }
    """

    # 调用生成函数
    try:
        image_url = generate_pose_image_url(sample_pose_json)
        print(f"\n生成的姿势示意图URL：{image_url}")
    except Exception as e:
        print(f"测试失败：{str(e)}")