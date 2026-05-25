import { useState, useEffect } from 'react';
import { notesApi } from '../api';

export default function NotesManager({ selectedSection, onNotesChanged }) {
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (selectedSection) {
      loadNotes();
    } else {
      setNotes([]);
    }
  }, [selectedSection]);

  const loadNotes = async () => {
    try {
      const res = await notesApi.getNotesBySection(selectedSection);
      setNotes(res.data);
    } catch (e) {
      console.error('加载笔记失败', e);
    }
  };

  const handleDelete = async (noteId) => {
    if (!confirm('确定要删除这条笔记吗？相关的复习内容也会被删除。')) return;
    try {
      await notesApi.deleteNote(noteId);
      loadNotes();
      onNotesChanged?.();
    } catch (err) {
      alert('删除失败: ' + (err.response?.data?.detail || err.message));
    }
  };

  const handleUpdate = async (noteId, file) => {
    setLoading(true);
    try {
      await notesApi.updateNote(noteId, file);
      loadNotes();
      onNotesChanged?.();
      alert('笔记更新成功！');
    } catch (err) {
      alert('更新失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  if (!selectedSection) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">笔记管理</h3>
        <p className="text-sm text-gray-500">请先在左侧选择一个板块</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-semibold text-gray-900">
          {selectedSection} 的笔记
        </h3>
        <span className="text-sm text-gray-500">{notes.length} 个文件</span>
      </div>

      {notes.length === 0 ? (
        <p className="text-sm text-gray-500">该板块下暂无笔记</p>
      ) : (
        <div className="space-y-2 max-h-80 overflow-y-auto">
          {notes.map((note) => (
            <div
              key={note.id}
              className="flex items-center justify-between p-3 bg-gray-50 rounded-lg group hover:bg-gray-100 transition-colors"
            >
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">
                  {note.file_name}
                </p>
                <p className="text-xs text-gray-500">
                  {note.chunks_count} 个知识点
                </p>
              </div>
              <div className="flex items-center gap-1 ml-2">
                <label className="cursor-pointer p-1.5 rounded-md hover:bg-white hover:shadow-sm transition-all">
                  <input
                    type="file"
                    accept=".md"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files?.[0]) {
                        handleUpdate(note.id, e.target.files[0]);
                      }
                    }}
                  />
                  <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                  </svg>
                </label>
                <button
                  onClick={() => handleDelete(note.id)}
                  className="p-1.5 rounded-md hover:bg-white hover:shadow-sm transition-all"
                >
                  <svg className="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
