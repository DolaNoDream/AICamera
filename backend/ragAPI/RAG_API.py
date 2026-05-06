import os
from http import HTTPStatus
from pathlib import Path
from dashscope import Application
import base64


def encode_image_to_base64(image_path: str) -> str:
    """将图片文件编码为base64格式的data URL"""
    with open(image_path, 'rb') as f:
        image_data = f.read()

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


def main():
    # 图片路径
    image_path = Path(__file__).resolve().parent.parent / "image" / "mmexport1752976083598.jpg"

    if not image_path.exists():
        print(f"错误：图片文件不存在 - {image_path}")
        return

    # 将图片编码为 base64
    image_base64 = encode_image_to_base64(str(image_path))
    print(f"图片加载成功: {image_path.name}")
    print(f"图片大小: {len(image_base64)} 字符")

    # 构建 prompt
    prompt = f"给我一个活泼的姿势"

    # 调用阿里云应用
    print("\n正在调用阿里云应用...")
    response = Application.call(
        api_key="sk-40ed2e1b81f545308b6a92574e923706",
        app_id='7a93bb813d09459c92d96b3108f8a700',
        prompt=prompt,
        images=[image_base64]
    )

    if response.status_code != HTTPStatus.OK:
        print(f'request_id={response.request_id}')
        print(f'code={response.status_code}')
        print(f'message={response.message}')
        print(f'请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code')
    else:
        print(f"\n应用返回结果:")
        print(response.output.text)


if __name__ == '__main__':
    main()
