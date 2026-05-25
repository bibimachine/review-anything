from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel
from ..database import get_db
from ..models import Section, Note

router = APIRouter(prefix="/api/sections", tags=["sections"])

# 系统预设板块（已废弃，不再自动创建）
DEFAULT_SECTION_NAME = None


class SectionCreate(BaseModel):
    name: str


class SectionRename(BaseModel):
    old_name: str
    new_name: str


@router.get("/")
def list_sections(db: Session = Depends(get_db)):
    """获取所有板块（包括手动创建的）"""
    db_sections = db.query(Section).all()
    note_sections = db.query(Note.section).distinct().all()
    all_sections = set(s.name for s in db_sections)
    all_sections.update(s[0] for s in note_sections)
    return sorted(list(all_sections))


@router.post("/")
def create_section(data: SectionCreate, db: Session = Depends(get_db)):
    """手动创建板块"""
    if not data.name or not data.name.strip():
        raise HTTPException(status_code=400, detail="板块名称不能为空")
    name = data.name.strip()
    existing = db.query(Section).filter(Section.name == name).first()
    if existing:
        raise HTTPException(status_code=400, detail="板块已存在")
    section = Section(name=name, is_system=False)
    db.add(section)
    db.commit()
    db.refresh(section)
    return {"id": section.id, "name": section.name}


@router.post("/rename")
def rename_section(data: SectionRename, db: Session = Depends(get_db)):
    """重命名板块（会同时更新 Section 表和 Note 表中的板块名）"""
    old_name = data.old_name.strip()
    new_name = data.new_name.strip()
    
    if not old_name or not new_name:
        raise HTTPException(status_code=400, detail="板块名称不能为空")
    if old_name == new_name:
        raise HTTPException(status_code=400, detail="新名称与旧名称相同")
    
    # 检查新名称是否已存在
    existing = db.query(Section).filter(Section.name == new_name).first()
    if existing:
        raise HTTPException(status_code=400, detail="目标板块名称已存在")
    
    # 更新 Section 表
    section = db.query(Section).filter(Section.name == old_name).first()
    if section:
        section.name = new_name
    
    # 更新 Note 表中的所有相关笔记
    db.query(Note).filter(Note.section == old_name).update({"section": new_name})
    
    db.commit()
    return {"message": "板块已重命名", "old_name": old_name, "new_name": new_name}


@router.delete("/{section_name}")
def delete_section(section_name: str, db: Session = Depends(get_db)):
    """删除板块及其下的所有笔记、分块和复习项"""
    # 先删除 Section 表记录
    section = db.query(Section).filter(Section.name == section_name).first()
    if section:
        db.delete(section)
    
    # 删除该板块下的所有笔记（级联删除 chunks 和 review_items）
    notes = db.query(Note).filter(Note.section == section_name).all()
    deleted_count = len(notes)
    for note in notes:
        db.delete(note)
    
    db.commit()
    return {"message": "板块已删除", "deleted_notes": deleted_count}
