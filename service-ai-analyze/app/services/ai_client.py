import httpx
import json
import base64
import os
import logging
import re

logger = logging.getLogger(__name__)

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")

# 创建共享的 transport，避免连接池问题
_async_transport = httpx.AsyncHTTPTransport(retries=0)


async def chat_completion(messages: list, model: str = "qwen2.5:7b", api_url: str = None, model_type: str = "ollama", api_key: str = None) -> str:
    """通用聊天补全接口，支持 Ollama 和 OpenAI 兼容 API"""
    url = api_url or OLLAMA_BASE_URL
    url = url.rstrip("/")

    headers = {}
    if model_type == "ollama":
        url += "/api/chat"
        body = {
            "model": model,
            "messages": messages,
            "stream": False
        }
    else:
        url += "/v1/chat/completions"
        body = {
            "model": model,
            "messages": messages,
            "stream": False
        }
        if api_key:
            headers["Authorization"] = f"Bearer {api_key}"

    logger.info(f"[chat_completion] Calling: {url}, model={model}, model_type={model_type}, messages={len(messages)}")
    async with httpx.AsyncClient(timeout=120.0, transport=_async_transport) as client:
        response = await client.post(url, json=body, headers=headers)
        if response.status_code != 200:
            logger.error(f"[chat_completion] API returned {response.status_code}: {response.text}")
        response.raise_for_status()
        result = response.json()

        if model_type == "ollama":
            msg = result.get("message", {})
            content = msg.get("content", "")
            if not content:
                logger.warning(f"[chat_completion] Ollama returned empty content. Response: {result}")
            return content
        else:
            choices = result.get("choices", [])
            if choices:
                content = choices[0].get("message", {}).get("content", "")
                if not content:
                    logger.warning(f"[chat_completion] OpenAI API returned empty content. Response: {result}")
                return content
            logger.warning(f"[chat_completion] OpenAI API returned no choices. Response: {result}")
            return ""


async def chat_completion_with_images(messages: list, model: str = "qwen2.5vl:7b", api_url: str = None) -> str:
    """支持图片的聊天补全接口，使用 Ollama vision 模型"""
    url = (api_url or OLLAMA_BASE_URL).rstrip("/") + "/api/chat"

    processed_messages = []
    for msg in messages:
        processed = {"role": msg.get("role", "user"), "content": msg.get("content", "")}
        if "images" in msg and msg["images"]:
            processed["images"] = msg["images"]
        processed_messages.append(processed)

    body = {
        "model": model,
        "messages": processed_messages,
        "stream": False
    }

    logger.info(f"[chat_completion_with_images] Calling: {url}, model={model}, messages={len(processed_messages)}")
    async with httpx.AsyncClient(timeout=120.0, transport=_async_transport) as client:
        response = await client.post(url, json=body)
        if response.status_code != 200:
            logger.error(f"[chat_completion_with_images] Ollama returned {response.status_code}: {response.text}")
        response.raise_for_status()
        result = response.json()
        msg = result.get("message", {})
        return msg.get("content", "")


async def analyze_image(image_path: str, prompt: str = None) -> dict:
    """分析图片，返回标签和位置信息"""
    # 验证文件路径，防止路径遍历攻击
    abs_path = os.path.abspath(image_path)
    if not os.path.exists(abs_path):
        raise FileNotFoundError(f"文件不存在: {image_path}")
    if not os.path.isfile(abs_path):
        raise ValueError(f"不是有效文件: {image_path}")
    
    with open(abs_path, "rb") as f:
        image_data = base64.b64encode(f.read()).decode("utf-8")

    default_prompt = """请分析这张照片，返回 JSON 格式的结果：
{
  "tags": ["标签1", "标签2", ...],
  "city": "城市名（如果能识别出拍摄地点）",
  "province": "省份名",
  "description": "一句话描述这张照片",
  "watermark_lat": null,
  "watermark_lng": null
}
注意：
- tags 是照片相关标签，如风景、建筑、人物、美食等，3-5个
- 如果无法识别地点，city 和 province 设为 null
- 【重要】请仔细观察照片中是否有水印文字，特别是包含经纬度/坐标信息的水印
- 常见水印经纬度格式举例：
  • "31.2304°N, 121.4737°E" 或 "31.2304°S, 121.4737°W"
  • "N31°14'25.4\" E121°28'21.3\""
  • "纬度:31.23 经度:121.47" 或 "Lat:31.23 Lng:121.47"
  • "31.2304, 121.4737"（纯数字逗号分隔）
  • "GPS: 31.230417, 121.473694"
  • 水印中可能出现的中文："拍摄于"、"地点"、"位置"后跟坐标
- 如果识别到经纬度，请将纬度填入 watermark_lat，经度填入 watermark_lng
- N/北纬 为正数，S/南纬 为负数；E/东经 为正数，W/西经 为负数
- 如果没有水印或无法识别经纬度，watermark_lat 和 watermark_lng 设为 null
- 只返回 JSON，不要其他文字"""

    async with httpx.AsyncClient(timeout=120.0, transport=_async_transport) as client:
        response = await client.post(
            f"{OLLAMA_BASE_URL}/api/generate",
            json={
                "model": "qwen2.5vl:7b",
                "prompt": prompt or default_prompt,
                "images": [image_data],
                "stream": False,
                "options": {"temperature": 0.3, "num_predict": 1024}
            }
        )
        if response.status_code != 200:
            logger.error(f"[analyze_image] Ollama returned {response.status_code}: {response.text}")
        response.raise_for_status()
        result = response.json()
        response_text = result.get("response", "")

        try:
            json_str = response_text.strip()
            # 使用正则表达式提取 JSON 内容，处理各种 markdown 代码块格式
            json_match = re.search(r'```(?:json)?\s*\n?(.*?)\n?\s*```', json_str, re.DOTALL)
            if json_match:
                json_str = json_match.group(1).strip()
            parsed = json.loads(json_str)
            # 记录水印经纬度识别结果
            if parsed.get("watermark_lat") and parsed.get("watermark_lng"):
                logger.info(f"[analyze_image] 水印经纬度识别成功: lat={parsed['watermark_lat']}, lng={parsed['watermark_lng']}")
            return parsed
        except json.JSONDecodeError:
            return {"tags": ["AI识别"], "city": None, "province": None, "description": response_text[:200]}
