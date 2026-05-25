import os
import zipfile
import shutil
import asyncio
import json
from fastapi import APIRouter, Depends, UploadFile, File, HTTPException
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from ..database import get_db
from ..models import Note, Chunk, ReviewItem
from ..services.parser import parse_markdown, extract_section_from_path, compute_hash
from ..services.llm import generate_multiple_qa, get_fallback_count

router = APIRouter(prefix="/api/notes", tags=["notes"])

# 全局进度状态（简单实现，单用户场景）
_upload_progress = {"current": 0, "total": 0, "current_file": "", "phase": "idle"}
_cancel_event = asyncio.Event()


def set_progress(current, total, current_file=""):
    _upload_progress["current"] = current
    _upload_progress["total"] = total
    _upload_progress["current_file"] = current_file


def set_phase(phase: str):
    _upload_progress["phase"] = phase


def reset_progress():
    _upload_progress["current"] = 0
    _upload_progress["total"] = 0
    _upload_progress["current_file"] = ""
    _upload_progress["phase"] = "idle"
    _cancel_event.clear()


def check_cancelled():
    if _cancel_event.is_set():
        raise HTTPException(status_code=499, detail="上传已取消")


@router.post("/upload-cancel")
async def upload_cancel():
    """取消当前上传/处理任务"""
    _cancel_event.set()
    set_phase("cancelled")
    return {"message": "已取消"}


@router.get("/upload-progress")
async def upload_progress():
    """SSE 推送上传/QA 生成进度"""
    async def event_generator():
        last_progress = None
        while True:
            current = _upload_progress["current"]
            total = _upload_progress["total"]
            current_file = _upload_progress["current_file"]
            phase = _upload_progress["phase"]
            progress = (current, total, current_file, phase)
            if progress != last_progress:
                last_progress = progress
                data = {
                    "type": "progress",
                    "phase": phase,
                    "current": current,
                    "total": total,
                    "current_file": current_file,
                }
                yield f"data: {json.dumps(data)}\n\n"
            # 如果完成或取消，再发一次然后结束
            if phase in ("done", "cancelled"):
                yield f"data: {json.dumps(data)}\n\n"
                break
            await asyncio.sleep(0.3)
    return StreamingResponse(event_generator(), media_type="text/event-stream")


async def process_chunks_for_note(note: Note, chunks_data: list, db: Session):
    """
    为笔记处理分块：
    - 如果分块内容哈希未变，保留原有复习项（保留复习状态）
    - 如果分块内容哈希变了或不存在，串行调用LLM生成多个QA
    - LLM失败时自动降级使用本地规则生成
    """
    # 建立现有chunk的哈希映射，用于复用
    existing_chunks = {c.content_hash: c for c in note.chunks}
    
    # 先统计需要处理的 chunk 数量
    chunks_to_process = []
    for chunk_data in chunks_data:
        content = chunk_data["content"]
        heading_path = chunk_data["heading_path"]
        content_hash = compute_hash(content)
        
        existing = existing_chunks.get(content_hash)
        if existing and existing.heading_path == heading_path:
            continue
        chunks_to_process.append(chunk_data)
    
    total = len(chunks_to_process)
    set_progress(0, total, note.file_name)
    set_phase("generating")
    
    # 串行处理每个需要生成 QA 的 chunk
    for idx, chunk_data in enumerate(chunks_to_process):
        check_cancelled()
        
        content = chunk_data["content"]
        heading_path = chunk_data["heading_path"]
        content_hash = compute_hash(content)
        
        # 内容变了或不存在，创建新chunk
        chunk = Chunk(
            note_id=note.id,
            content=content,
            content_hash=content_hash,
            heading_path=heading_path
        )
        db.add(chunk)
        db.flush()
        
        # 更新进度
        set_progress(idx + 1, total, f"{note.file_name} - {heading_path or '无标题'}")
        
        # 串行调用 LLM 生成 QA
        qa_list = await generate_multiple_qa(content, heading_path, db, max_qa=3)
        for qa in qa_list:
            review_item = ReviewItem(
                chunk_id=chunk.id,
                question=qa["question"],
                answer=qa["answer"],
                llm_failed=False
            )
            db.add(review_item)


@router.post("/upload")
async def upload_notes(
    file: UploadFile = File(...),
    section: str = None,
    db: Session = Depends(get_db)
):
    """上传笔记文件或zip包，支持指定板块"""
    if not file.filename.endswith('.zip'):
        raise HTTPException(status_code=400, detail="请上传zip文件")
    
    reset_progress()
    
    # 保存上传的zip文件
    upload_dir = "./uploads"
    os.makedirs(upload_dir, exist_ok=True)
    zip_path = os.path.join(upload_dir, file.filename)
    
    set_phase("uploading")
    try:
        with open(zip_path, "wb") as f:
            content = await file.read()
            f.write(content)
    except Exception as e:
        reset_progress()
        raise HTTPException(status_code=500, detail=f"文件保存失败: {str(e)}")
    
    check_cancelled()
    
    # 解压
    set_phase("extracting")
    extract_dir = os.path.join(upload_dir, "extracted")
    if os.path.exists(extract_dir):
        shutil.rmtree(extract_dir)
    os.makedirs(extract_dir)
    
    try:
        with zipfile.ZipFile(zip_path, 'r') as zip_ref:
            zip_ref.extractall(extract_dir)
    except Exception as e:
        reset_progress()
        # 清理并返回错误
        if os.path.exists(zip_path):
            os.remove(zip_path)
        if os.path.exists(extract_dir):
            shutil.rmtree(extract_dir)
        raise HTTPException(status_code=400, detail=f"zip 解压失败: {str(e)}")
    
    check_cancelled()
    
    # 解析所有markdown文件
    set_phase("parsing")
    parsed_notes = []
    for root, dirs, files in os.walk(extract_dir):
        for filename in files:
            if filename.endswith('.md'):
                file_path = os.path.join(root, filename)
                rel_path = os.path.relpath(file_path, extract_dir)
                
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # 如果指定了板块，使用指定板块；否则优先用 zip 文件名作为板块名
                file_section = section if section else extract_section_from_path(rel_path, file.filename)
                content_hash = compute_hash(content)
                
                # 检查是否已存在相同路径和哈希的笔记
                existing_note = db.query(Note).filter(
                    Note.file_path == rel_path
                ).first()
                
                if existing_note and existing_note.content_hash == content_hash:
                    # 笔记内容未变，跳过
                    parsed_notes.append({
                        "file_name": filename,
                        "section": file_section,
                        "chunks_count": len(existing_note.chunks),
                        "status": "unchanged"
                    })
                    continue
                
                if existing_note:
                    # 笔记存在但内容变了，删除旧的分块和复习项，重新处理
                    for chunk in list(existing_note.chunks):
                        db.delete(chunk)
                    db.flush()
                    existing_note.content = content
                    existing_note.content_hash = content_hash
                    existing_note.file_name = filename
                    existing_note.section = file_section
                    note = existing_note
                else:
                    # 新笔记
                    note = Note(
                        file_path=rel_path,
                        file_name=filename,
                        section=file_section,
                        content=content,
                        content_hash=content_hash
                    )
                    db.add(note)
                    db.flush()
                
                check_cancelled()
                
                # 解析分块并处理
                chunks = parse_markdown(content)
                await process_chunks_for_note(note, chunks, db)
                
                parsed_notes.append({
                    "file_name": filename,
                    "section": file_section,
                    "chunks_count": len(chunks),
                    "status": "updated" if existing_note else "created"
                })
    
    check_cancelled()
    
    # 写入数据库
    set_phase("writing")
    db.commit()
    
    # 清理
    shutil.rmtree(extract_dir)
    os.remove(zip_path)
    
    # 获取LLM fallback统计
    fallback_count = get_fallback_count()
    
    set_phase("done")
    
    return {
        "message": "上传成功",
        "notes": parsed_notes,
        "fallback_count": fallback_count
    }


@router.get("/sections")
def get_sections(db: Session = Depends(get_db)):
    """获取所有板块"""
    sections = db.query(Note.section).distinct().all()
    return [s[0] for s in sections]


@router.get("/section/{section_name}")
def get_notes_by_section(section_name: str, db: Session = Depends(get_db)):
    """获取某板块下的所有笔记"""
    notes = db.query(Note).filter(Note.section == section_name).all()
    return [
        {
            "id": n.id,
            "file_name": n.file_name,
            "file_path": n.file_path,
            "section": n.section,
            "content": n.content,
            "chunks_count": len(n.chunks)
        }
        for n in notes
    ]


@router.delete("/{note_id}")
def delete_note(note_id: int, db: Session = Depends(get_db)):
    """删除笔记及其所有分块和复习项"""
    note = db.query(Note).filter(Note.id == note_id).first()
    if not note:
        raise HTTPException(status_code=404, detail="笔记不存在")
    db.delete(note)
    db.commit()
    return {"message": "笔记已删除"}


@router.post("/regenerate-qa")
async def regenerate_qa(section: str = None, db: Session = Depends(get_db)):
    """
    使用当前模型重新生成所有 QA。
    如果指定了 section，只更新该板块下的笔记。
    返回更新统计。
    """
    from ..services.llm import generate_multiple_qa

    query = db.query(Note)
    if section:
        query = query.filter(Note.section == section)

    notes = query.all()
    updated_notes = 0
    updated_chunks = 0

    for note in notes:
        # 删除旧 chunks 和 review_items
        for chunk in list(note.chunks):
            db.delete(chunk)
        db.flush()

        # 重新解析并生成 QA
        chunks = parse_markdown(note.content)
        for chunk_data in chunks:
            content = chunk_data["content"]
            heading_path = chunk_data["heading_path"]
            content_hash = compute_hash(content)

            chunk = Chunk(
                note_id=note.id,
                content=content,
                content_hash=content_hash,
                heading_path=heading_path
            )
            db.add(chunk)
            db.flush()

            qa_list = await generate_multiple_qa(content, heading_path, db, max_qa=3)
            for qa in qa_list:
                review_item = ReviewItem(
                    chunk_id=chunk.id,
                    question=qa["question"],
                    answer=qa["answer"],
                    llm_failed=False
                )
                db.add(review_item)
            updated_chunks += 1

        updated_notes += 1

    db.commit()
    reset_progress()
    return {
        "message": "QA 重新生成完成",
        "updated_notes": updated_notes,
        "updated_chunks": updated_chunks,
        "section": section or "全部"
    }


@router.put("/{note_id}")
async def update_note(note_id: int, file: UploadFile = File(...), db: Session = Depends(get_db)):
    """更新笔记：重新上传单个markdown文件替换原有内容"""
    note = db.query(Note).filter(Note.id == note_id).first()
    if not note:
        raise HTTPException(status_code=404, detail="笔记不存在")
    
    if not file.filename.endswith('.md'):
        raise HTTPException(status_code=400, detail="请上传markdown文件")
    
    content = (await file.read()).decode('utf-8')
    new_hash = compute_hash(content)
    
    # 如果内容没变，直接返回
    if note.content_hash == new_hash:
        return {
            "message": "笔记内容未变更",
            "file_name": note.file_name,
            "chunks_count": len(note.chunks)
        }
    
    # 删除旧的分块和复习项
    for chunk in list(note.chunks):
        db.delete(chunk)
    db.flush()
    
    # 更新笔记内容
    note.content = content
    note.content_hash = new_hash
    note.file_name = file.filename
    db.flush()
    
    # 重新解析分块并处理（会复用未变更的chunk的QA）
    chunks = parse_markdown(content)
    await process_chunks_for_note(note, chunks, db)
    
    db.commit()
    return {
        "message": "笔记已更新",
        "file_name": note.file_name,
        "chunks_count": len(chunks)
    }
