from typing import Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from PIL import Image
from PIL.ExifTags import TAGS, GPSTAGS

router = APIRouter()

class ExifResponse(BaseModel):
    gps_lat: Optional[float] = None
    gps_lng: Optional[float] = None
    shoot_time: Optional[str] = None
    width: Optional[int] = None
    height: Optional[int] = None

def _convert_to_degrees(value):
    try:
        d = float(value[0])
        m = float(value[1])
        s = float(value[2])
        return d + (m / 60.0) + (s / 3600.0)
    except (TypeError, IndexError, ZeroDivisionError):
        return None

def _get_gps_info(img):
    try:
        exif_data = img.getexif()
        if not exif_data:
            return None, None

        gps_info = {}
        for tag_id, value in exif_data.items():
            tag = TAGS.get(tag_id, tag_id)
            if tag == "GPSInfo":
                for gps_tag_id, gps_value in value.items():
                    gps_tag = GPSTAGS.get(gps_tag_id, gps_tag_id)
                    gps_info[gps_tag] = gps_value
                break

        lat = None
        lng = None
        if "GPSLatitude" in gps_info and "GPSLatitudeRef" in gps_info:
            lat = _convert_to_degrees(gps_info["GPSLatitude"])
            if gps_info["GPSLatitudeRef"] == "S":
                lat = -lat

        if "GPSLongitude" in gps_info and "GPSLongitudeRef" in gps_info:
            lng = _convert_to_degrees(gps_info["GPSLongitude"])
            if gps_info["GPSLongitudeRef"] == "W":
                lng = -lng

        return lat, lng
    except Exception:
        return None, None

def _get_shoot_time(img):
    try:
        exif_data = img.getexif()
        if not exif_data:
            return None
        for tag_id, value in exif_data.items():
            tag = TAGS.get(tag_id, tag_id)
            if tag in ("DateTimeOriginal", "DateTimeDigitized", "DateTime"):
                return str(value)
        return None
    except Exception:
        return None

@router.post("/parse-exif", response_model=ExifResponse)
async def parse_exif(image_path: str):
    try:
        with Image.open(image_path) as img:
            lat, lng = _get_gps_info(img)
            shoot_time = _get_shoot_time(img)

            return ExifResponse(
                gps_lat=lat,
                gps_lng=lng,
                shoot_time=shoot_time,
                width=img.width,
                height=img.height
            )
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="文件不存在")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
