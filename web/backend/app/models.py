from sqlalchemy import Column, Integer, String, Text, DateTime, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime
from .database import Base


class Config(Base):
    __tablename__ = "configs"

    id = Column(Integer, primary_key=True, index=True)
    api_base_url = Column(String, nullable=True)
    api_key = Column(String, nullable=True)
    model_name = Column(String, default="gpt-3.5-turbo")
    daily_review_count = Column(Integer, default=10)
    created_at = Column(DateTime, default=datetime.utcnow)


class Section(Base):
    __tablename__ = "sections"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False, unique=True)
    is_system = Column(Boolean, default=False)  # 系统预设板块，不可删除
    created_at = Column(DateTime, default=datetime.utcnow)


class Note(Base):
    __tablename__ = "notes"

    id = Column(Integer, primary_key=True, index=True)
    file_path = Column(String, nullable=False)
    file_name = Column(String, nullable=False)
    section = Column(String, nullable=False)  # 板块/文件夹名
    content = Column(Text, nullable=False)
    content_hash = Column(String(64), default="")  # 内容哈希，用于判断是否需要重新生成QA
    created_at = Column(DateTime, default=datetime.utcnow)

    chunks = relationship("Chunk", back_populates="note", cascade="all, delete-orphan")


class Chunk(Base):
    __tablename__ = "chunks"

    id = Column(Integer, primary_key=True, index=True)
    note_id = Column(Integer, ForeignKey("notes.id"), nullable=False)
    content = Column(Text, nullable=False)
    content_hash = Column(String(64), default="")  # 内容哈希
    heading_path = Column(String, default="")  # 标题路径，如 "第一章/第一节"
    created_at = Column(DateTime, default=datetime.utcnow)

    note = relationship("Note", back_populates="chunks")
    review_items = relationship("ReviewItem", back_populates="chunk", cascade="all, delete-orphan")


class ReviewItem(Base):
    __tablename__ = "review_items"

    id = Column(Integer, primary_key=True, index=True)
    chunk_id = Column(Integer, ForeignKey("chunks.id"), nullable=False)
    question = Column(Text, nullable=False)
    answer = Column(Text, nullable=False)
    is_hard = Column(Boolean, default=False)  # 易忘标记
    review_count = Column(Integer, default=0)
    next_review_at = Column(DateTime, default=datetime.utcnow)
    last_reviewed_at = Column(DateTime, nullable=True)
    llm_failed = Column(Boolean, default=False)  # LLM生成是否失败
    created_at = Column(DateTime, default=datetime.utcnow)

    chunk = relationship("Chunk", back_populates="review_items")


class CheckIn(Base):
    __tablename__ = "checkins"

    id = Column(Integer, primary_key=True, index=True)
    checkin_date = Column(String(10), nullable=False, unique=True)  # 格式: YYYY-MM-DD
    created_at = Column(DateTime, default=datetime.utcnow)
