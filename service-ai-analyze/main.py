from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import thumbnail, exif_parser, chat
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[
        logging.FileHandler("ai_service.log", encoding="utf-8"),
        logging.StreamHandler()
    ]
)

app = FastAPI(title="Smart-NAS AI Service", version="1.0.0")

# 添加 CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(thumbnail.router, prefix="/api/ai", tags=["thumbnail"])
app.include_router(exif_parser.router, prefix="/api/ai", tags=["exif"])
app.include_router(chat.router, prefix="/api/ai", tags=["chat"])

@app.get("/health")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
