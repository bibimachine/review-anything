import asyncio
import json
import re
from sqlalchemy.orm import Session
from openai import AsyncOpenAI
from ..models import Config

# 全局限制并发数，避免 API 限速
_llm_semaphore = asyncio.Semaphore(2)


async def get_llm_config(db: Session) -> dict:
    """获取LLM配置，自动兼容 OpenAI SDK 风格的 base_url"""
    config = db.query(Config).first()
    if not config:
        return None
    
    # 兼容 OpenAI SDK 风格的 base_url（不含 /chat/completions）
    api_base_url = config.api_base_url
    if api_base_url and not api_base_url.endswith('/chat/completions'):
        api_base_url = api_base_url.rstrip('/') + '/chat/completions'
    
    return {
        "api_base_url": api_base_url,
        "api_key": config.api_key,
        "model_name": config.model_name
    }


def parse_json_response(content: str) -> dict:
    """从LLM响应中解析JSON"""
    try:
        return json.loads(content)
    except json.JSONDecodeError:
        # 尝试从markdown代码块中提取
        json_match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', content, re.DOTALL)
        if json_match:
            return json.loads(json_match.group(1))
        else:
            # 尝试找到第一个{和最后一个}
            start = content.find('{')
            end = content.rfind('}')
            if start != -1 and end != -1:
                return json.loads(content[start:end+1])
            else:
                raise Exception("无法解析LLM返回的JSON")


def generate_fallback_qa(chunk_content: str, heading_path: str, max_qa: int = 3) -> list:
    """
    当LLM调用失败时，使用本地规则生成简单的复习问题
    基于标题和内容的启发式生成
    """
    qa_list = []
    
    # 使用标题路径生成问题
    if heading_path:
        parts = heading_path.split('/')
        title = parts[-1] if parts else heading_path
        
        # 问题1：概念解释
        qa_list.append({
            "question": f"请解释「{title}」的含义或作用？",
            "answer": chunk_content[:300] if len(chunk_content) > 300 else chunk_content
        })
        
        # 问题2：如果有父标题，问关系
        if len(parts) > 1:
            parent = parts[-2]
            qa_list.append({
                "question": f"「{title}」与「{parent}」之间有什么关系？",
                "answer": chunk_content[:300] if len(chunk_content) > 300 else chunk_content
            })
    else:
        # 没有标题路径，用内容前几个字
        preview = chunk_content[:50].replace('\n', ' ')
        qa_list.append({
            "question": f"关于「{preview}...」，请简述其核心要点？",
            "answer": chunk_content[:300] if len(chunk_content) > 300 else chunk_content
        })
    
    # 问题3：从内容中提取关键句子作为问题
    sentences = re.split(r'[。\.\n]', chunk_content)
    key_sentences = [s.strip() for s in sentences if len(s.strip()) > 10 and len(s.strip()) < 100]
    if key_sentences and len(qa_list) < max_qa:
        key = key_sentences[0]
        qa_list.append({
            "question": f"如何理解这句话：「{key}」？",
            "answer": chunk_content[:300] if len(chunk_content) > 300 else chunk_content
        })
    
    return qa_list[:max_qa]


async def generate_qa(chunk_content: str, heading_path: str, db: Session) -> dict:
    """
    调用大模型生成一个问题和答案
    返回: {"question": str, "answer": str}
    """
    config = await get_llm_config(db)
    if not config:
        raise ValueError("LLM配置未设置")
    
    system_prompt = """你是一个帮助用户复习笔记的助手。请根据提供的笔记内容生成一个复习问题及其答案。
要求：
1. 问题应该考察用户对知识点的理解，而不是简单的记忆
2. 答案应该简洁准确，覆盖核心要点
3. 请用JSON格式返回，格式如下：
{"question": "问题内容", "answer": "答案内容"}
"""
    
    context = f"笔记标题路径: {heading_path}\n\n笔记内容:\n{chunk_content}" if heading_path else f"笔记内容:\n{chunk_content}"
    
    client = AsyncOpenAI(
        api_key=config["api_key"],
        base_url=config["api_base_url"].replace("/chat/completions", ""),
    )
    response = await client.chat.completions.create(
        model=config["model_name"],
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": context},
        ],
        temperature=0.7,
        reasoning_effort="high",
        extra_body={"thinking": {"type": "enabled"}},
    )
    content = response.choices[0].message.content
    result = parse_json_response(content)
    
    print(f"[LLM] 生成成功: {heading_path or '无标题'} -> 1 个QA")
    return {
        "question": result.get("question", ""),
        "answer": result.get("answer", "")
    }


# 全局计数器，用于统计fallback次数
_fallback_counter = {"count": 0}


def get_fallback_count() -> int:
    """获取并重置fallback计数"""
    count = _fallback_counter["count"]
    _fallback_counter["count"] = 0
    return count


def increment_fallback():
    """增加fallback计数"""
    _fallback_counter["count"] += 1


async def generate_multiple_qa(chunk_content: str, heading_path: str, db: Session, max_qa: int = 3) -> list:
    """
    调用大模型为chunk生成多个复习问题和答案
    如果LLM调用失败，自动降级使用本地规则生成
    返回: [{"question": str, "answer": str}, ...]
    """
    config = await get_llm_config(db)
    if not config:
        # 没有配置，直接降级
        increment_fallback()
        return generate_fallback_qa(chunk_content, heading_path, max_qa)
    
    system_prompt = f"""你是一个帮助用户复习笔记的助手。请根据提供的笔记内容生成{max_qa}个不同的复习问题及其答案。
要求：
1. 每个问题考察不同的知识点或角度
2. 问题应该考察理解，而不是简单记忆
3. 答案简洁准确，覆盖核心要点
4. 请用JSON格式返回数组，格式如下：
[{{"question": "问题1", "answer": "答案1"}}, {{"question": "问题2", "answer": "答案2"}}]
"""
    
    context = f"笔记标题路径: {heading_path}\n\n笔记内容:\n{chunk_content}" if heading_path else f"笔记内容:\n{chunk_content}"
    
    try:
        client = AsyncOpenAI(
            api_key=config["api_key"],
            base_url=config["api_base_url"].replace("/chat/completions", ""),
        )
        response = await client.chat.completions.create(
            model=config["model_name"],
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": context},
            ],
            temperature=0.8,
            reasoning_effort="high",
            extra_body={"thinking": {"type": "enabled"}},
        )
        content = response.choices[0].message.content
        
        # 尝试解析JSON数组
        try:
            result = json.loads(content)
        except json.JSONDecodeError:
            # 尝试从markdown代码块中提取
            json_match = re.search(r'```(?:json)?\s*(\[.*?\])\s*```', content, re.DOTALL)
            if json_match:
                result = json.loads(json_match.group(1))
            else:
                # 尝试找到第一个[和最后一个]
                start = content.find('[')
                end = content.rfind(']')
                if start != -1 and end != -1:
                    result = json.loads(content[start:end+1])
                else:
                    raise Exception("无法解析LLM返回的JSON数组")
        
        if not isinstance(result, list):
            # 如果返回的是单个对象，包装成列表
            if isinstance(result, dict) and "question" in result:
                result = [result]
            else:
                raise Exception("LLM返回格式不正确")
        
        qa_list = []
        for item in result[:max_qa]:
            if isinstance(item, dict) and "question" in item and "answer" in item:
                qa_list.append({
                    "question": item["question"],
                    "answer": item["answer"]
                })
        
        if not qa_list:
            raise Exception("LLM没有返回有效的问题")
        
        print(f"[LLM] 生成成功: {heading_path or '无标题'} -> {len(qa_list)} 个QA")
        return qa_list
    except Exception as e:
        print(f"[LLM] 生成失败 [{heading_path or '无标题'}]: {e}")
        increment_fallback()
        return generate_fallback_qa(chunk_content, heading_path, max_qa)
