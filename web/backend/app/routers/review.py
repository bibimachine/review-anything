from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from ..database import get_db
from ..models import ReviewItem, Chunk, Note
from ..services.llm import generate_multiple_qa

router = APIRouter(prefix="/api/review", tags=["review"])


class ReviewAction(BaseModel):
    action: str  # "remember" 或 "forget"


async def retry_failed_llm_items(db: Session, section: str = None, limit: int = 3):
    """
    尝试重新生成之前LLM失败的问题
    返回成功重试的数量
    """
    query = db.query(ReviewItem).join(Chunk).join(Note).filter(ReviewItem.llm_failed == True)
    
    if section:
        query = query.filter(Note.section == section)
    
    failed_items = query.limit(limit).all()
    retry_count = 0
    
    for item in failed_items:
        chunk = item.chunk
        try:
            qa_list = await generate_multiple_qa(chunk.content, chunk.heading_path, db, max_qa=3)
            if qa_list:
                # 更新第一个失败项为成功
                item.question = qa_list[0]["question"]
                item.answer = qa_list[0]["answer"]
                item.llm_failed = False
                retry_count += 1
                
                # 如果生成了多个问题，为同一个chunk创建额外的复习项
                for qa in qa_list[1:]:
                    extra_item = ReviewItem(
                        chunk_id=chunk.id,
                        question=qa["question"],
                        answer=qa["answer"],
                        llm_failed=False
                    )
                    db.add(extra_item)
        except Exception as e:
            print(f"重试LLM生成失败: {e}")
            # 保持llm_failed=True，下次继续尝试
    
    if retry_count > 0:
        db.commit()
    
    return retry_count


def _item_to_dict(review_item: ReviewItem) -> dict:
    """将 ReviewItem 转换为返回字典"""
    chunk = review_item.chunk
    note = chunk.note
    return {
        "id": review_item.id,
        "question": review_item.question,
        "answer": review_item.answer,
        "is_hard": review_item.is_hard,
        "review_count": review_item.review_count,
        "note": {
            "file_name": note.file_name,
            "section": note.section,
            "heading_path": chunk.heading_path,
            "file_path": note.file_path
        }
    }


async def _fetch_review_items(db: Session, section: str = None, limit: int = 1) -> list:
    """按优先级获取复习项列表"""
    # 先尝试重试之前LLM生成失败的项目
    await retry_failed_llm_items(db, section)
    
    query = db.query(ReviewItem).join(Chunk).join(Note)
    
    if section:
        query = query.filter(Note.section == section)
    
    now = datetime.utcnow()
    items = []
    
    # 优先级1：易忘项
    hard_items = query.filter(
        ReviewItem.is_hard == True,
        ReviewItem.llm_failed == False
    ).order_by(ReviewItem.last_reviewed_at.asc().nullsfirst()).limit(limit).all()
    items.extend(hard_items)
    
    # 优先级2：到期的
    if len(items) < limit:
        due_items = query.filter(
            ReviewItem.next_review_at <= now,
            ReviewItem.llm_failed == False,
            ReviewItem.is_hard == False
        ).order_by(ReviewItem.next_review_at.asc()).limit(limit - len(items)).all()
        # 去重
        existing_ids = {item.id for item in items}
        for item in due_items:
            if item.id not in existing_ids:
                items.append(item)
    
    # 优先级3：新项
    if len(items) < limit:
        new_items = query.filter(
            ReviewItem.review_count == 0,
            ReviewItem.llm_failed == False,
            ReviewItem.is_hard == False
        ).limit(limit - len(items)).all()
        existing_ids = {item.id for item in items}
        for item in new_items:
            if item.id not in existing_ids:
                items.append(item)
    
    return items


@router.get("/next")
async def get_next_review(section: str = None, db: Session = Depends(get_db)):
    """获取下一个要复习的条目"""
    items = await _fetch_review_items(db, section, limit=1)
    
    if not items:
        return {"message": "没有更多需要复习的内容了", "item": None}
    
    return {
        "message": "ok",
        "item": _item_to_dict(items[0])
    }


@router.get("/batch")
async def get_batch_review(section: str = None, count: int = 10, db: Session = Depends(get_db)):
    """批量获取复习条目"""
    items = await _fetch_review_items(db, section, limit=count)
    
    if not items:
        return {"message": "没有更多需要复习的内容了", "items": []}
    
    return {
        "message": "ok",
        "items": [_item_to_dict(item) for item in items]
    }


@router.post("/{item_id}/action")
def review_action(item_id: int, action: ReviewAction, db: Session = Depends(get_db)):
    """处理复习动作"""
    item = db.query(ReviewItem).filter(ReviewItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=404, detail="复习项不存在")
    
    now = datetime.utcnow()
    item.last_reviewed_at = now
    item.review_count += 1
    
    if action.action == "forget":
        # 标记为易忘，1小时后再次复习
        item.is_hard = True
        item.next_review_at = now + timedelta(hours=1)
    elif action.action == "remember":
        # 取消易忘标记，按间隔重复算法安排下次复习
        item.is_hard = False
        # 简单间隔：1天, 3天, 7天, 14天, 30天
        intervals = [1, 3, 7, 14, 30]
        idx = min(item.review_count - 1, len(intervals) - 1)
        item.next_review_at = now + timedelta(days=intervals[idx])
    else:
        raise HTTPException(status_code=400, detail="无效的操作")
    
    db.commit()
    
    return {
        "message": "操作成功",
        "next_review_at": item.next_review_at.isoformat(),
        "is_hard": item.is_hard
    }


@router.get("/stats")
def get_stats(db: Session = Depends(get_db)):
    """获取复习统计"""
    total = db.query(ReviewItem).count()
    hard = db.query(ReviewItem).filter(ReviewItem.is_hard == True).count()
    today_due = db.query(ReviewItem).filter(
        ReviewItem.next_review_at <= datetime.utcnow(),
        ReviewItem.llm_failed == False
    ).count()
    failed = db.query(ReviewItem).filter(ReviewItem.llm_failed == True).count()
    
    return {
        "total": total,
        "hard": hard,
        "today_due": today_due,
        "failed": failed
    }
