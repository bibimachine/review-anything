import re
import os
import hashlib


MAX_CHUNK_SIZE = 2000  # 每个chunk最大字符数（放宽限制）


def compute_hash(text: str) -> str:
    """计算文本的SHA256哈希"""
    return hashlib.sha256(text.encode('utf-8')).hexdigest()


def split_by_paragraphs(text: str, heading_path: str) -> list:
    """
    将文本按段落切分，保持段落完整性。
    一个段落作为一个 chunk，不从中切开。
    只有单段超过 MAX_CHUNK_SIZE 时才被迫拆分。
    """
    # 先按段落分割（空行分隔）
    paragraphs = [p.strip() for p in text.split('\n\n') if p.strip()]
    
    if not paragraphs:
        if text.strip():
            return [{"heading_path": heading_path, "content": text.strip()}]
        return []
    
    chunks = []
    current_content = ""
    
    for para in paragraphs:
        # 如果当前累积内容加上新段落会超限，先保存当前累积
        if current_content and len(current_content) + len(para) + 2 > MAX_CHUNK_SIZE:
            chunks.append({
                "heading_path": heading_path,
                "content": current_content.strip()
            })
            current_content = ""
        
        # 如果单个段落就超过限制，被迫拆分（按句子边界）
        if len(para) > MAX_CHUNK_SIZE:
            # 先保存之前累积的内容
            if current_content:
                chunks.append({
                    "heading_path": heading_path,
                    "content": current_content.strip()
                })
                current_content = ""
            
            # 按句子切分，尽量保持句子完整
            sentences = re.split(r'([。\.\?\!？！]\s*)', para)
            sentence_buffer = ""
            for i in range(0, len(sentences), 2):
                sentence = sentences[i]
                sep = sentences[i + 1] if i + 1 < len(sentences) else ""
                full_sentence = sentence + sep
                
                if len(sentence_buffer) + len(full_sentence) > MAX_CHUNK_SIZE:
                    if sentence_buffer:
                        chunks.append({
                            "heading_path": heading_path,
                            "content": sentence_buffer.strip()
                        })
                    sentence_buffer = full_sentence
                else:
                    sentence_buffer += full_sentence
            
            if sentence_buffer:
                chunks.append({
                    "heading_path": heading_path,
                    "content": sentence_buffer.strip()
                })
        else:
            # 段落未超限，累积
            if current_content:
                current_content = current_content + "\n\n" + para
            else:
                current_content = para
    
    # 保存最后剩余的内容
    if current_content:
        chunks.append({
            "heading_path": heading_path,
            "content": current_content.strip()
        })
    
    return chunks


def parse_markdown(content: str) -> list:
    """
    将Markdown内容按标题分块。
    每个标题下的完整内容作为一个 chunk，不从中切开。
    只有单段内容超过 MAX_CHUNK_SIZE 时才被迫按句子拆分。
    返回: [{"heading_path": str, "content": str}, ...]
    """
    lines = content.split('\n')
    raw_chunks = []
    current_heading_path = []
    current_content = []
    
    heading_pattern = re.compile(r'^(#{1,6})\s+(.+)$')
    
    def save_raw_chunk():
        if current_content:
            # 去掉标题行本身，只保留正文内容
            text = '\n'.join(current_content).strip()
            if text:
                raw_chunks.append({
                    "heading_path": '/'.join(current_heading_path) if current_heading_path else "",
                    "content": text
                })
    
    for line in lines:
        match = heading_pattern.match(line)
        if match:
            save_raw_chunk()
            level = len(match.group(1))
            title = match.group(2).strip()
            # 调整标题路径
            while len(current_heading_path) >= level:
                current_heading_path.pop()
            current_heading_path.append(title)
            # 标题行不放入内容，从下一行开始
            current_content = []
        else:
            current_content.append(line)
    
    save_raw_chunk()
    
    # 如果没有分块（没有标题），则整体作为一个块
    if not raw_chunks and content.strip():
        raw_chunks.append({
            "heading_path": "",
            "content": content.strip()
        })
    
    # 按标题分块后的内容，保持段落完整性
    final_chunks = []
    for rc in raw_chunks:
        chunks = split_by_paragraphs(rc["content"], rc["heading_path"])
        final_chunks.extend(chunks)
    
    return final_chunks


def extract_section_from_path(file_path: str, zip_name: str = None) -> str:
    """从文件路径提取板块名（文件夹名）
    如果提供了 zip_name，优先使用 zip 文件名（去掉 .zip 后缀）作为板块名
    """
    if zip_name:
        # 去掉 .zip 后缀作为板块名
        base = zip_name.replace('\\', '/').split('/')[-1]
        if base.lower().endswith('.zip'):
            base = base[:-4]
        if base:
            return base
    
    parts = file_path.replace('\\', '/').split('/')
    # 去掉文件名，取倒数第二个文件夹名
    if len(parts) >= 2:
        return parts[-2] if parts[-2] else "默认板块"
    return "默认板块"
