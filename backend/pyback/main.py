import uvicorn
import json
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional
# 注意：确保这两个模块的路径正确，能正常导入
from GeneratePose import generate_pose_suggestions
from GeneratePic import generate_pose_image_url

# ======================== 1. 初始化 FastAPI 应用 ========================
app = FastAPI(
    title="PoseSug 服务",
    description="处理 /posesug 接口的姿势建议后端服务",
    version="1.0.0"
)


# ======================== 2. 定义数据模型（请求/响应结构） ========================
# 新响应模型：大幅简化，只返回 sessionId、姿势示意图URL、构图指导文字、语音播报音频URL
class PosesugResponse(BaseModel):
    sessionId: str
    poseImageUrl: str  # 姿势示意图URL（由模型生成）
    guideText: str  # 构图指导文字（一段完整的文字描述）
    voiceAudioText: str  # 语音播报WAV文件URL（假设后续生成或存储后返回URL）
    poseSuggestions: list[dict]


# ======================== 3. 核心接口实现 ========================
@app.post(
    "/posesug",
    response_model=PosesugResponse,
    summary="姿势建议生成接口",
    description="接收图像和会话参数，返回姿势示意图、构图指导文字和语音播报音频"
)
async def posesug(
        # 必填参数（输入内容保持完全不变）
        sessionId: str = Form(..., description="会话唯一标识"),
        image: UploadFile = File(..., description="待分析的图像文件（JPG/PNG）"),
        # 可选参数
        userIntent: Optional[str] = Form(None, description="用户拍摄意图（如：全身照 显腿长）"),
        meta: Optional[str] = Form(None, description="相机元数据（JSON字符串）")
):
    """
    /posesug 接口核心处理逻辑
    """
    # -------------------------- 步骤1：参数校验 --------------------------
    # 校验sessionId非空
    if not sessionId.strip():
        raise HTTPException(status_code=400, detail="参数错误：sessionId不能为空")

    # 校验图像格式（仅支持JPG/PNG）
    allowed_extensions = ["jpg", "jpeg", "png"]
    file_ext = image.filename.split(".")[-1].lower() if image.filename else ""
    if file_ext not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"参数错误：仅支持JPG/PNG格式图像，当前文件格式为{file_ext}"
        )

    # 校验meta参数（如果传了则必须是合法JSON）
    meta_dict = {}
    if meta:
        try:
            meta_dict = json.loads(meta)
        except json.JSONDecodeError:
            raise HTTPException(status_code=400, detail="参数错误：meta不是合法的JSON字符串")

    # -------------------------- 步骤2：读取图像文件（二进制） --------------------------
    # 读取图像二进制数据（供后续业务逻辑使用）
    try:
        image_data = await image.read()
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"图像读取失败：{str(e)}")

    # -------------------------- 步骤3：核心业务逻辑 --------------------------
    try:
        # 1. 调用姿势建议生成函数（返回JSON字符串）
        suggestion_result = generate_pose_suggestions(image_data, userIntent, meta_dict)

        if not isinstance(suggestion_result, (str, bytes, bytearray)):
            raise TypeError(f"generate_pose_suggestions 返回值类型错误，期望str/bytes，实际是{type(suggestion_result)}")
        # 2. 关键修复：将JSON字符串解析为Python字典
        suggestion = json.loads(suggestion_result)

        # 3. 生成姿势示意图URL（传入poseSuggestions列表）
        pose_image_url = generate_pose_image_url(suggestion["poseSuggestions"])

        # 4. 构造构图指导文字（将compositionGuide字典转为可读字符串）
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
        # 捕获业务逻辑中的所有异常，返回标准化错误
        raise HTTPException(status_code=500, detail=f"业务处理失败：{str(e)}")

    # -------------------------- 步骤4：构造并返回响应 --------------------------
    return PosesugResponse(
        sessionId=sessionId,
        poseImageUrl=pose_image_url,
        guideText=guide_text,
        voiceAudioText=voice_audio_text,
        poseSuggestions=pose_suggestions_list
    )


# ======================== 4. 异常处理 ========================
@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    """统一处理HTTP异常，返回标准化JSON响应"""
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )


# ======================== 5. 启动服务 ========================
if __name__ == "__main__":
    # 启动服务：地址0.0.0.0，端口9001
    uvicorn.run(
        app="main:app",  # main是当前文件名，app是FastAPI实例名
        host="0.0.0.0",
        port=9001,
        reload=True  # 开发模式下自动重载，生产环境关闭
    )