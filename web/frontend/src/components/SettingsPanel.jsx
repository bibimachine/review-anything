import { useState, useEffect } from 'react';
import { configApi, notesApi, sectionsApi } from '../api';

export default function SettingsPanel({ onConfigDeleted, dailyCount, onDailyCountChange, onRegenerateQA }) {
  const [config, setConfig] = useState(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [editingCount, setEditingCount] = useState(false);
  const [newCount, setNewCount] = useState(dailyCount);
  const [sections, setSections] = useState([]);
  const [selectedSectionForQA, setSelectedSectionForQA] = useState('');
  const [qaUpdating, setQAUpdating] = useState(false);

  useEffect(() => {
    loadConfig();
    loadSections();
  }, []);

  useEffect(() => {
    setNewCount(dailyCount);
  }, [dailyCount]);

  const loadConfig = async () => {
    try {
      const res = await configApi.get();
      if (res.data.configured) {
        setConfig(res.data.config);
        onDailyCountChange?.(res.data.config.daily_review_count || 10);
      }
    } catch (e) {
      console.error('加载配置失败', e);
    }
  };

  const loadSections = async () => {
    try {
      const res = await sectionsApi.list();
      setSections(res.data.filter((s) => s));
    } catch (e) {
      console.error('加载板块失败', e);
    }
  };

  const handleDelete = async () => {
    setLoading(true);
    try {
      await configApi.delete();
      setConfig(null);
      setShowConfirm(false);
      onConfigDeleted?.();
    } catch (err) {
      alert('删除失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleSaveCount = async () => {
    const count = parseInt(newCount);
    if (isNaN(count) || count < 1 || count > 100) {
      alert('请输入 1-100 之间的数字');
      return;
    }
    setLoading(true);
    try {
      await configApi.save({
        api_base_url: config.api_base_url,
        api_key: '', // 不修改 key
        model_name: config.model_name,
        daily_review_count: count,
      });
      onDailyCountChange?.(count);
      setEditingCount(false);
      await loadConfig();
    } catch (err) {
      alert('保存失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleRegenerateQA = async () => {
    const section = selectedSectionForQA || null;
    const targetName = section || '全部';
    if (!confirm(`确定要使用当前模型重新生成「${targetName}」板块的 QA 吗？\n\n这会删除现有的 QA 并重新生成。`)) {
      return;
    }
    setQAUpdating(true);
    try {
      const res = await notesApi.regenerateQA(section);
      alert(`QA 更新完成！\n更新了 ${res.data.updated_notes} 个笔记，${res.data.updated_chunks} 个分块`);
    } catch (err) {
      alert('更新失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setQAUpdating(false);
    }
  };

  if (!config) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">模型配置</h3>
        <p className="text-sm text-gray-500">尚未配置大模型 API</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 模型配置 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">模型配置</h3>
        <div className="space-y-3">
          <div className="flex justify-between items-center py-2 border-b border-gray-100">
            <span className="text-sm text-gray-500">API Base URL</span>
            <span className="text-sm font-medium text-gray-900 truncate max-w-xs">
              {config.api_base_url}
            </span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-gray-100">
            <span className="text-sm text-gray-500">模型名称</span>
            <span className="text-sm font-medium text-gray-900">{config.model_name}</span>
          </div>
        </div>

        {!showConfirm ? (
          <button
            onClick={() => setShowConfirm(true)}
            className="mt-4 w-full py-2 border border-red-300 text-red-600 rounded-lg text-sm font-medium hover:bg-red-50 transition-colors"
          >
            删除配置
          </button>
        ) : (
          <div className="mt-4 p-3 bg-red-50 rounded-lg">
            <p className="text-sm text-red-700 mb-3">确定要删除模型配置吗？删除后需要重新配置才能使用。</p>
            <div className="flex gap-2">
              <button
                onClick={handleDelete}
                disabled={loading}
                className="flex-1 py-2 bg-red-600 text-white rounded-lg text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
              >
                {loading ? '删除中...' : '确认删除'}
              </button>
              <button
                onClick={() => setShowConfirm(false)}
                className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
              >
                取消
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 复习设置 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">复习设置</h3>

        {!editingCount ? (
          <div className="flex justify-between items-center">
            <div>
              <p className="text-sm font-medium text-gray-900">每日复习数量</p>
              <p className="text-sm text-gray-500">每次复习时加载的题目数量</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-lg font-bold text-blue-600">{dailyCount} 题</span>
              <button
                onClick={() => setEditingCount(true)}
                className="px-3 py-1.5 text-sm text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors"
              >
                修改
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                每日复习数量（1-100）
              </label>
              <input
                type="number"
                min={1}
                max={100}
                value={newCount}
                onChange={(e) => setNewCount(parseInt(e.target.value) || 10)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              />
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleSaveCount}
                disabled={loading}
                className="flex-1 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
              >
                {loading ? '保存中...' : '保存'}
              </button>
              <button
                onClick={() => {
                  setEditingCount(false);
                  setNewCount(dailyCount);
                }}
                className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg text-sm font-medium hover:bg-gray-50 transition-colors"
              >
                取消
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 更新 QA */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">🔄 更新 QA</h3>
        <p className="text-sm text-gray-500 mb-4">
          使用当前配置的模型重新生成 QA。可以选择更新全部或指定板块。
        </p>

        <div className="space-y-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              选择板块
            </label>
            <select
              value={selectedSectionForQA}
              onChange={(e) => setSelectedSectionForQA(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none text-sm"
            >
              <option value="">全部板块</option>
              {sections.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>

          <button
            onClick={handleRegenerateQA}
            disabled={qaUpdating}
            className="w-full py-2.5 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 disabled:opacity-50 transition-colors"
          >
            {qaUpdating ? '更新中...' : '开始更新 QA'}
          </button>
        </div>
      </div>
    </div>
  );
}
