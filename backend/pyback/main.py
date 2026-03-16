import uvicorn
import json
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional, Dict, Any

# 注意：确保这两个模块的路径正确，能正常导入
from GeneratePose import generate_pose_suggestions
from GeneratePic import generate_pose_image_url
from GeneratePictureEdit import generate_edited_image_url

# ======================== 1. 初始化 FastAPI 应用 ========================
app = FastAPI(
    title="PoseSug & AI Picture 服务",
    description="姿势建议 + AI 一键 p 图 后端服务",
    version="1.0.0"
)


# ======================== 2. 数据模型（请求/响应结构） ========================

# ── 原有姿势建议响应 ────────────────────────────────────────────────
class PosesugResponse(BaseModel):
    sessionId: str
    poseImageUrl: str
    guideText: str
    voiceAudioText: str
    poseSuggestions: list[dict]


# ── 新增：AI p图 响应模型 ───────────────────────────────────────────
class AIPictureResponse(BaseModel):
    code: int
    msg: str
    imageUrl: Optional[str] = None  # 处理后的图片公网可访问 URL


# 用于接收和校验 p图需求
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


# ======================== 3. 接口实现 ========================

# ── 原有接口：/posesug ───────────────────────────────────────────────
@app.post(
    "/posesug",
    response_model=PosesugResponse,
    summary="姿势建议生成接口",
    description="接收图像和会话参数，返回姿势示意图、构图指导文字和语音播报音频"
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
            raise TypeError(f"generate_pose_suggestions 返回值类型错误，期望str/bytes，实际是{type(suggestion_result)}")

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


# ── 新增接口：/ai/picture ────────────────────────────────────────────
@app.post(
    "/ai/picture",
    response_model=AIPictureResponse,
    summary="AI 一键 p 图接口",
    description="上传单张照片 + p图需求，返回处理后的图片URL"
)
async def ai_picture(
        sessionId: str = Form(..., description="会话唯一标识"),
        image: UploadFile = File(..., description="待处理的单张照片（JPG/PNG）"),
        requirement: str = Form(..., description="JSON格式的p图需求，必填，至少包含一个有效字段")
):
    # 1. 基础参数校验
    if not sessionId.strip():
        raise HTTPException(400, detail="sessionId 不能为空")

    allowed_ext = {".jpg", ".jpeg", ".png"}
    ext = (image.filename or "").lower()
    if not any(ext.endswith(e) for e in allowed_ext):
        raise HTTPException(400, detail="仅支持 jpg / jpeg / png 格式")

    # 2. 解析 requirement
    try:
        req_dict = json.loads(requirement)
        req_model = PictureRequirement(**req_dict)

        if not req_model.has_any_requirement():
            raise ValueError("至少需要指定一项 p 图需求（filter/portrait/background/special）")

    except json.JSONDecodeError:
        raise HTTPException(400, detail="requirement 不是有效的 JSON 字符串")
    except Exception as e:
        raise HTTPException(400, detail=f"requirement 参数格式错误：{str(e)}")

    # 3. 读取原图
    try:
        image_bytes = await image.read()
    except Exception as e:
        raise HTTPException(400, detail=f"无法读取上传图片：{str(e)}")

    # 4. AI p图核心处理（你来实现）
    try:
        # ── TODO: 你需要实现的真实 p 图逻辑 ─────────────────────────────
        # 返回值建议：处理后的图片 bytes
        processed_url = generate_edited_image_url(
            image_bytes=image_bytes,
            requirement=req_model.dict(exclude_none=True),  # 或直接 req_model.model_dump()
            filename=image.filename or "uploaded.jpg"
        )


        return AIPictureResponse(
            code=200,
            msg="success",
            imageUrl=processed_url
        )

    except Exception as e:
        raise HTTPException(500, detail=f"AI p图处理失败：{str(e)}")



# ======================== 4. 异常处理 ========================
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )


# ======================== 5. 启动服务 ========================
if __name__ == "__main__":
    uvicorn.run(
        app="main:app",  # 假设文件名为 main.py
        host="0.0.0.0",
        port=9001,
        reload=True  # 生产环境建议改为 False
    )