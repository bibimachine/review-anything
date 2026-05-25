from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from .database import engine, Base
from .routers import config, notes, review, sections, checkin

# 创建数据库表
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Review Anything API", version="1.0.0")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
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
