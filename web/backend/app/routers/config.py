import asyncio
import aiohttp
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from ..database import get_db
from ..models import Config as ConfigModel

router = APIRouter(prefix="/api/config", tags=["config"])


class ConfigCreate(BaseModel):
    model_config = {"protected_namespaces": ()}
    api_base_url: str | None = None
    api_key: str | None = None
    model_name: str = "gpt-3.5-turbo"
    daily_review_count: int = 10


class ConfigResponse(BaseModel):
    model_config = {"protected_namespaces": (), "from_attributes": True}
    id: int
    api_base_url: str
    model_name: str
    daily_review_count: int


@router.get("/", response_model=dict)
def get_config(db: Session = Depends(get_db)):
    config = db.query(ConfigModel).first()
    if not config:
        return {"configured": False, "has_llm": False}
    has_llm = bool(config.api_base_url and config.api_key)
    return {
        "configured": True,
        "has_llm": has_llm,
        "config": ConfigResponse.from_orm(config)
    }


@router.post("/", response_model=ConfigResponse)
def create_or_update_config(config_data: ConfigCreate, db: Session = Depends(get_db)):
    config = db.query(ConfigModel).first()
    if config:
        if config_data.api_base_url is not None:
            config.api_base_url = config_data.api_base_url
        if config_data.api_key is not None:
            config.api_key = config_data.api_key
        config.model_name = config_data.model_name
        config.daily_review_count = config_data.daily_review_count
    else:
        config = ConfigModel(
            api_base_url=config_data.api_base_url or "",
            api_key=config_data.api_key or "",
            model_name=config_data.model_name,
            daily_review_count=config_data.daily_review_count
        )
        db.add(config)
    db.commit()
    db.refresh(config)
    return config


@router.post("/test")
async def test_config_connection(config_data: ConfigCreate):
    """测试 LLM API 连接是否正常"""
    base_url = config_data.api_base_url
    # 兼容 OpenAI SDK 风格的 base_url（不含 /chat/completions）
    if base_url and not base_url.endswith('/chat/completions'):
        base_url = base_url.rstrip('/') + '/chat/completions'
    api_key = config_data.api_key
    model_name = config_data.model_name

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    payload = {
        "model": model_name,
        "messages": [{"role": "user", "content": "Hi"}],
        "max_tokens": 5
    }

    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                base_url,
                headers=headers,
                json=payload,
                timeout=aiohttp.ClientTimeout(total=10)
            ) as response:
                if response.status == 200:
                    return {"success": True, "message": "连接成功"}
                elif response.status == 401:
                    return {"success": False, "message": "API Key 无效（401）"}
                else:
                    text = await response.text()
                    return {"success": False, "message": f"API 错误: {response.status} - {text[:100]}"}
    except aiohttp.ClientConnectorError:
        return {"success": False, "message": "无法连接到 API 服务器，请检查 Base URL"}
    except asyncio.TimeoutError:
        return {"success": False, "message": "连接超时，请检查网络或 API 地址"}
    except Exception as e:
        return {"success": False, "message": f"连接失败: {str(e)}"}


@router.delete("/")
def delete_config(db: Session = Depends(get_db)):
    """删除模型配置"""
    config = db.query(ConfigModel).first()
    if not config:
        raise HTTPException(status_code=404, detail="配置不存在")
    db.delete(config)
    db.commit()
    return {"message": "配置已删除"}
