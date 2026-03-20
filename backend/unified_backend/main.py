import json
from pathlib import Path
from typing import Optional, List

import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

# 先加载 .env，再导入依赖 key 的模块
ROOT_DIR = Path(__file__).resolve().parents[2]
load_dotenv(ROOT_DIR / ".env")

from GeneratePose import generate_pose_suggestions
from GeneratePic import generate_pose_image_url
from GenerateWrite import generate_ai_write
from GeneratePictureEdit import generate_edited_image_url



app = FastAPI(
    title="Unified AI Backend Service",
    description="姿势建议 + AI成文 + AI一键P图 统一后端服务",
    version="1.0.0"
)


# ======================== 响应模型 ========================

class PosesugResponse(BaseModel):
    sessionId: str
    poseImageUrl: str
    guideText: str
    voiceAudioText: str
    poseSuggestions: list[dict]


class StandardResponse(BaseModel):
    code: int
    msg: str
    data: dict


class AIPictureResponse(BaseModel):
    code: int
    msg: str
    imageUrl: Optional[str] = None


class PictureRequirement(BaseModel):
    filter: Optional[str] = None
    portrait: Optional[str] = None
    background: Optional[str] = None
    special: Optional[str] = None

    def has_any_requirement(self) -> bool:
        return any([
            self.filter is not None,
            self.portrait is not None,
            self.background is not None,
            self.special is not None
        ])


# ======================== 接口 1：姿势建议 ========================

@app.post(
    "/posesug",
    response_model=PosesugResponse,
    summary="姿势建议生成接口",
    description="接收图像和会话参数，返回姿势示意图、构图指导文字和语音播报内容"
)
async def posesug(
    sessionId: str = Form(..., description="会话唯一标识"),
    image: UploadFile = File(..., description="待分析的图像文件（JPG/PNG）"),
    userIntent: Optional[str] = Form(None, description="用户拍摄意图（如：全身照 显腿长）"),
    meta: Optional[str] = Form(None, description="相机元数据（JSON字符串）")
):
    if not sessionId.strip():
        raise HTTPException(status_code=400, detail="参数错误：sessionId不能为空")

    allowed_extensions = ["jpg", "jpeg", "png"]
    file_ext = image.filename.split(".")[-1].lower() if image.filename else ""
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"参数错误：仅支持JPG/PNG格式图像，当前文件格式为{file_ext}"
        )

    meta_dict = {}
    if meta:
        try:
            meta_dict = json.loads(meta)
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="参数错误：meta不是合法的JSON字符串")

    try:
        image_data = await image.read()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"图像读取失败：{str(e)}")

    try:
        suggestion_result = generate_pose_suggestions(image_data, userIntent, meta_dict)

        if not isinstance(suggestion_result, (str, bytes, bytearray)):
            raise TypeError(
                f"generate_pose_suggestions 返回值类型错误，期望str/bytes，实际是{type(suggestion_result)}"
            )

        suggestion = json.loads(suggestion_result)
        pose_image_url = generate_pose_image_url(suggestion["poseSuggestions"])

        composition_guide = suggestion["compositionGuide"]
        guide_text = (
            f"构图建议：{composition_guide['framing']}；拍摄角度：{composition_guide['angle']}；"
            f"背景处理：{composition_guide['background']}；构图平衡：{composition_guide['symmetry']}"
        )

        voice_audio_text = suggestion["voiceGuide"]
        pose_suggestions_list = suggestion["poseSuggestions"]

    except json.JSONDecodeError as e:
        raise HTTPException(status_code=500, detail=f"姿势建议解析失败：{str(e)}")
    except KeyError as e:
        raise HTTPException(status_code=500, detail=f"姿势建议数据缺失字段：{str(e)}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"业务处理失败：{str(e)}")

    return PosesugResponse(
        sessionId=sessionId,
        poseImageUrl=pose_image_url,
        guideText=guide_text,
        voiceAudioText=voice_audio_text,
        poseSuggestions=pose_suggestions_list
    )


# ======================== 接口 2：AI成文 ========================

@app.post(
    "/ai/write",
    response_model=StandardResponse,
    summary="AI成文接口",
    description="上传单张/多张照片及需求参数，生成相应的社交文案"
)
async def ai_write_api(
    sessionId: str = Form(..., description="会话唯一标识"),
    image: List[UploadFile] = File(..., description="上传的多张照片数组"),
    requirement: Optional[str] = Form(None, description="成文需求（JSON格式字符串）")
):
    if not sessionId.strip():
        return StandardResponse(code=400, msg="参数错误：sessionId不能为空", data={})

    req_dict = {}
    if requirement:
        try:
            req_dict = json.loads(requirement)
        except json.JSONDecodeError:
            return StandardResponse(code=400, msg="参数错误：requirement必须是合法的JSON字符串", data={})

    image_bytes_list = []
    try:
        for img in image:
            file_ext = img.filename.split(".")[-1].lower() if img.filename else ""
            if file_ext not in ["jpg", "jpeg", "png", "webp"]:
                return StandardResponse(code=400, msg=f"不支持的图片格式: {file_ext}", data={})

            img_bytes = await img.read()
            image_bytes_list.append(img_bytes)

        if not image_bytes_list:
            return StandardResponse(code=400, msg="未读取到有效的图片内容", data={})

    except Exception as e:
        return StandardResponse(code=400, msg=f"图片读取失败: {str(e)}", data={})

    try:
        content = generate_ai_write(
            image_data_list=image_bytes_list,
            requirement=req_dict
        )

        return StandardResponse(
            code=200,
            msg="success",
            data={"content": content}
        )

    except Exception as e:
        return StandardResponse(
            code=500,
            msg=f"服务端处理失败: {str(e)}",
            data={}
        )


# ======================== 接口 3：AI一键P图 ========================

@app.post(
    "/ai/picture",
    response_model=AIPictureResponse,
    summary="AI 一键P图接口",
    description="上传单张照片 + P图需求，返回处理后的图片URL"
)
async def ai_picture(
    sessionId: str = Form(..., description="会话唯一标识"),
    image: UploadFile = File(..., description="待处理的单张照片（JPG/PNG）"),
    requirement: str = Form(..., description="JSON格式的P图需求")
):
    if not sessionId.strip():
        raise HTTPException(status_code=400, detail="sessionId 不能为空")

    allowed_ext = {".jpg", ".jpeg", ".png"}
    filename = (image.filename or "").lower()
    if not any(filename.endswith(ext) for ext in allowed_ext):
        raise HTTPException(status_code=400, detail="仅支持 jpg / jpeg / png 格式")

    try:
        req_dict = json.loads(requirement)
        req_model = PictureRequirement(**req_dict)

        if not req_model.has_any_requirement():
            raise ValueError("至少需要指定一项P图需求（filter/portrait/background/special）")

    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="requirement 不是有效的 JSON 字符串")
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"requirement 参数格式错误：{str(e)}")

    try:
        image_bytes = await image.read()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"无法读取上传图片：{str(e)}")

    try:
        processed_url = generate_edited_image_url(
            image_bytes=image_bytes,
            requirement=req_model.dict(exclude_none=True),
            filename=image.filename or "uploaded.jpg"
        )

        return AIPictureResponse(
            code=200,
            msg="success",
            imageUrl=processed_url
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI P图处理失败：{str(e)}")


# ======================== 统一异常处理 ========================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )


# ======================== 启动服务 ========================

if __name__ == "__main__":
    uvicorn.run(
        app="main:app",
        host="0.0.0.0",
        port=9001,
        reload=True
    )