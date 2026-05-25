import { useState } from 'react';
import { reviewApi } from '../api';

export default function ReviewCard({ item, onAction, currentIndex, totalCount }) {
  const [showAnswer, setShowAnswer] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  if (!item) {
    return (
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
        <div className="text-6xl mb-4">🎉</div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">今日复习完成！</h3>
        <p className="text-gray-500">你已经完成了所有待复习的内容</p>
      </div>
    );
  }

  const handleAction = async (action) => {
    setActionLoading(true);
    try {
      await reviewApi.action(item.id, action);
      setShowAnswer(false);
      onAction?.(action);
    } catch (err) {
      alert('操作失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setActionLoading(false);
    }
  };

  const handleReveal = () => {
    setShowAnswer(true);
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      {/* 进度条 */}
      <div className="px-6 pt-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm text-gray-500">
            进度 {currentIndex + 1} / {totalCount}
          </span>
          <span className="text-sm text-gray-500">
            {Math.round(((currentIndex + 1) / totalCount) * 100)}%
          </span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-2">
          <div
            className="bg-blue-600 h-2 rounded-full transition-all duration-300"
            style={{ width: `${((currentIndex + 1) / totalCount) * 100}%` }}
          />
        </div>
      </div>

      {/* 问题区域 */}
      <div className="p-6">
        <div className="flex items-center gap-2 mb-4">
          <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs font-medium rounded-full">
            {item.note.section}
          </span>
          {item.is_hard && (
            <span className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
              易忘
            </span>
          )}
          {item.note.heading_path && (
            <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-full">
              {item.note.heading_path}
            </span>
          )}
        </div>

        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          {item.question}
        </h2>

        {!showAnswer && (
          <p className="text-sm text-gray-400">
            回忆一下这部分内容，然后点击「查看答案」
          </p>
        )}
      </div>

      {/* 答案区域 */}
      {showAnswer && (
        <div className="px-6 pb-4 border-t border-gray-100 pt-4">
          <div className="bg-green-50 rounded-lg p-4 mb-3">
            <h4 className="text-sm font-semibold text-green-800 mb-1">答案</h4>
            <p className="text-sm text-green-700 whitespace-pre-wrap">{item.answer}</p>
          </div>
          <div className="bg-blue-50 rounded-lg p-3">
            <h4 className="text-xs font-semibold text-blue-800 mb-1">笔记出处</h4>
            <p className="text-xs text-blue-700">
              文件: {item.note.file_name}
              {item.note.heading_path && ` / 位置: ${item.note.heading_path}`}
            </p>
          </div>
        </div>
      )}

      {/* 操作按钮 */}
      <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex gap-3">
        {!showAnswer ? (
          <>
            <button
              onClick={handleReveal}
              disabled={actionLoading}
              className="flex-1 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              👀 查看答案
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => handleAction('forget')}
              disabled={actionLoading}
              className="flex-1 py-2.5 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
            >
              😵 忘记了
            </button>
            <button
              onClick={() => handleAction('remember')}
              disabled={actionLoading}
              className="flex-1 py-2.5 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 transition-colors"
            >
              😊 记住了
            </button>
          </>
        )}
      </div>
    </div>
  );
}
