from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel, ConfigDict
from typing import List, Optional
from app.services.ai_client import chat_completion
import logging
import os
import json

logger = logging.getLogger(__name__)

router = APIRouter()

class ChatMessage(BaseModel):
    role: str
    content: str
    images: Optional[List[str]] = None

class ChatRequest(BaseModel):
    model_config = ConfigDict(protected_namespaces=())
    messages: List[ChatMessage]
    model: str = "qwen2.5:7b"
    api_url: Optional[str] = None
    model_type: str = "ollama"
    api_key: Optional[str] = None

class ChatResponse(BaseModel):
    content: str

class AnalyzeImageRequest(BaseModel):
    image_path: str
    prompt: Optional[str] = None

class AnalyzeImageResponse(BaseModel):
    tags: list
    city: Optional[str] = None
    province: Optional[str] = None
    description: Optional[str] = None
    watermark_lat: Optional[float] = None
    watermark_lng: Optional[float] = None

@router.post("/chat")
async def chat_raw(request: Request):
    raw_body = await request.body()
    # 记录请求信息时隐藏敏感字段
    try:
        parsed_log = json.loads(raw_body)
        if isinstance(parsed_log, dict) and "api_key" in parsed_log:
            parsed_log["api_key"] = "***"
        logger.info(f"[Chat] Request: {json.dumps(parsed_log, ensure_ascii=False)[:2000]}")
    except Exception:
        logger.info(f"[Chat] Raw request body ({len(raw_body)} bytes)")
    
    try:
        parsed = json.loads(raw_body)
    except Exception as e:
        logger.error(f"[Chat] Failed to parse body: {e}")
        raise HTTPException(status_code=422, detail=f"Invalid JSON: {e}")

    req = ChatRequest.model_validate(parsed)
    try:
        messages = []
        for m in req.messages:
            msg_dict = {"role": m.role, "content": m.content}
            if m.images:
                msg_dict["images"] = m.images
            messages.append(msg_dict)

        has_images = any(m.images for m in req.messages)
        model_to_use = req.model
        if has_images and "vl" not in model_to_use.lower():
            # 如果消息包含图片但模型不支持视觉，自动切换到视觉模型
            vision_model = os.getenv("VISION_MODEL", "qwen2.5vl:7b")
            logger.info(f"[Chat] Model '{model_to_use}' may not support images, switching to '{vision_model}'")
            model_to_use = vision_model

        content = await chat_completion(messages, model_to_use, req.api_url, req.model_type, req.api_key)
        return ChatResponse(content=content)
    except Exception as e:
        logger.error(f"[Chat] Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"AI 调用失败: {str(e)}")

@router.post("/analyze-image", response_model=AnalyzeImageResponse)
async def analyze_image(req: AnalyzeImageRequest):
    try:
        from app.services.ai_client import analyze_image as _analyze
        result = await _analyze(req.image_path, req.prompt)
        return AnalyzeImageResponse(**result)
    except Exception as e:
        logger.error(f"[analyze-image] Error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"AI 分析失败: {str(e)}")
