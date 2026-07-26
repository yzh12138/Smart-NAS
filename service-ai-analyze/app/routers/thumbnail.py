from typing import Tuple
import os
from pathlib import Path
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from PIL import Image

router = APIRouter()

class ThumbnailRequest(BaseModel):
    image_path: str
    thumbnail_path: str
    max_size: Tuple[int, int] = (300, 300)

class ThumbnailResponse(BaseModel):
    thumbnail_path: str
    width: int
    height: int

@router.post("/thumbnail", response_model=ThumbnailResponse)
async def generate_thumbnail(req: ThumbnailRequest):
    try:
        with Image.open(req.image_path) as img:
            img.thumbnail(req.max_size, Image.Resampling.LANCZOS)

            os.makedirs(os.path.dirname(req.thumbnail_path), exist_ok=True)
            img.save(req.thumbnail_path, quality=85, optimize=True)

            return ThumbnailResponse(
                thumbnail_path=req.thumbnail_path,
                width=img.width,
                height=img.height
            )
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="源文件不存在")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
