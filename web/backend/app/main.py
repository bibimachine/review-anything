import os
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from .database import engine, Base
from .routers import config, notes, review, sections, checkin

# 创建数据库表
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Review Anything API", version="1.0.0")

# 配置CORS（开发模式需要，生产模式下前后端同域可不依赖）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局异常处理：确保即使 500 错误也返回 JSON 并带 CORS 头
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"detail": f"服务器内部错误: {str(exc)}"},
    )

# 注册路由
app.include_router(config.router)
app.include_router(notes.router)
app.include_router(review.router)
app.include_router(sections.router)
app.include_router(checkin.router)


@app.get("/api/health")
def health_check():
    return {"status": "ok"}


# 挂载前端构建产物（如果存在）
_frontend_dist = os.path.join(os.path.dirname(__file__), "../../frontend/dist")
if os.path.isdir(_frontend_dist):
    app.mount("/", StaticFiles(directory=_frontend_dist, html=True), name="static")
