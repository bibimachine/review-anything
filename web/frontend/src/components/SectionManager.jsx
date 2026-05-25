import { useState, useEffect } from 'react';
import { sectionsApi } from '../api';

export default function SectionManager({ selectedSection, onSelectSection, onSectionsChanged }) {
  const [sections, setSections] = useState([]);
  const [newSectionName, setNewSectionName] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [editingSection, setEditingSection] = useState(null);
  const [editName, setEditName] = useState('');

  const loadSections = async () => {
    try {
      const res = await sectionsApi.list();
      setSections(res.data);
    } catch (e) {
      console.error('加载板块失败', e);
    }
  };

  useEffect(() => {
    loadSections();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    const name = newSectionName.trim();
    if (!name) return;
    setLoading(true);
    try {
      await sectionsApi.create(name);
      setNewSectionName('');
      setShowCreateForm(false);
      loadSections();
      onSectionsChanged?.();
    } catch (err) {
      alert('创建失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (sectionName) => {
    if (!confirm(`确定要删除板块「${sectionName}」吗？板块下的所有笔记和复习内容也会被删除。`)) return;
    try {
      await sectionsApi.delete(sectionName);
      if (selectedSection === sectionName) {
        onSelectSection?.(null);
      }
      loadSections();
      onSectionsChanged?.();
    } catch (err) {
      alert('删除失败: ' + (err.response?.data?.detail || err.message));
    }
  };

  const handleRename = async (oldName) => {
    const name = editName.trim();
    if (!name) {
      setEditingSection(null);
      return;
    }
    if (name === oldName) {
      setEditingSection(null);
      return;
    }
    setLoading(true);
    try {
      await sectionsApi.rename(oldName, name);
      if (selectedSection === oldName) {
        onSelectSection?.(name);
      }
      setEditingSection(null);
      setEditName('');
      loadSections();
      onSectionsChanged?.();
    } catch (err) {
      alert('重命名失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const startEdit = (sectionName) => {
    setEditingSection(sectionName);
    setEditName(sectionName);
  };

  const cancelEdit = () => {
    setEditingSection(null);
    setEditName('');
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">板块管理</h3>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          + 新建板块
        </button>
      </div>

      {showCreateForm && (
        <form onSubmit={handleCreate} className="mb-4 space-y-2">
          <input
            type="text"
            placeholder="输入板块名称"
            value={newSectionName}
            onChange={(e) => setNewSectionName(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            autoFocus
          />
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? '创建中...' : '创建'}
            </button>
            <button
              type="button"
              onClick={() => setShowCreateForm(false)}
              className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
            >
              取消
            </button>
          </div>
        </form>
      )}

      {sections.length === 0 ? (
        <p className="text-sm text-gray-500">暂无板块，点击上方按钮创建</p>
      ) : (
        <div className="space-y-1 max-h-60 overflow-y-auto">
          <button
            onClick={() => onSelectSection?.(null)}
            className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
              selectedSection === null
                ? 'bg-blue-100 text-blue-700'
                : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            全部板块
          </button>
          {sections.map((section) => (
            <div
              key={section}
              className={`flex items-center justify-between px-3 py-2 rounded-lg text-sm transition-colors ${
                selectedSection === section
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {editingSection === section ? (
                <div className="flex-1 flex items-center gap-2">
                  <input
                    type="text"
                    value={editName}
                    onChange={(e) => setEditName(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleRename(section);
                      if (e.key === 'Escape') cancelEdit();
                    }}
                    className="flex-1 px-2 py-1 text-sm border border-blue-300 rounded focus:ring-2 focus:ring-blue-500 outline-none"
                    autoFocus
                  />
                  <button
                    onClick={() => handleRename(section)}
                    disabled={loading}
                    className="p-1 rounded hover:bg-green-100 text-green-600 transition-colors"
                    title="确认"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                    </svg>
                  </button>
                  <button
                    onClick={cancelEdit}
                    className="p-1 rounded hover:bg-gray-200 text-gray-500 transition-colors"
                    title="取消"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              ) : (
                <>
                  <button
                    onClick={() => onSelectSection?.(section)}
                    className="flex-1 text-left font-medium"
                  >
                    {section}
                  </button>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => startEdit(section)}
                      className="p-1 rounded hover:bg-blue-100 text-gray-400 hover:text-blue-500 transition-colors"
                      title="重命名"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      onClick={() => handleDelete(section)}
                      className="p-1 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 transition-colors"
                      title="删除板块"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
