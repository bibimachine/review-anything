import { useState, useRef, useCallback } from 'react';
import { notesApi } from '../api';

const PHASE_CONFIG = {
  idle: { label: '等待上传', percent: 0 },
  uploading: { label: '正在上传文件...', percent: 10 },
  extracting: { label: '正在解压...', percent: 25 },
  parsing: { label: '正在解析笔记...', percent: 40 },
  generating: { label: '正在生成 QA...', percent: 40 },
  writing: { label: '正在写入数据库...', percent: 95 },
  done: { label: '处理完成', percent: 100 },
  cancelled: { label: '已取消', percent: 0 },
};

export default function UploadPanel({ onUploadSuccess, selectedSection }) {
  const [dragActive, setDragActive] = useState(false);
  const [uploadPhase, setUploadPhase] = useState('idle');
  const [uploadMessage, setUploadMessage] = useState('');
  const [fallbackCount, setFallbackCount] = useState(0);
  const [phase, setPhase] = useState('idle');
  const [overallProgress, setOverallProgress] = useState(0);
  const [qaProgress, setQaProgress] = useState({ current: 0, total: 0, currentFile: '' });
  const [isCancelling, setIsCancelling] = useState(false);
  const inputRef = useRef(null);
  const abortControllerRef = useRef(null);
  const sseRef = useRef(null);

  const resetAll = useCallback(() => {
    setUploadPhase('idle');
    setUploadMessage('');
    setPhase('idle');
    setOverallProgress(0);
    setQaProgress({ current: 0, total: 0, currentFile: '' });
    setIsCancelling(false);
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
    }
    abortControllerRef.current = null;
  }, []);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFile(e.dataTransfer.files[0]);
    }
  };

  const handleChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleCancel = async () => {
    if (isCancelling) return;
    setIsCancelling(true);
    setUploadMessage('正在取消...');

    // 通知后端取消
    try {
      await notesApi.cancelUpload();
    } catch {
      // ignore
    }

    // 取消前端请求
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    // 关闭 SSE
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
    }

    resetAll();
    setUploadPhase('error');
    setUploadMessage('已取消上传');
  };

  const handleFile = async (file) => {
    if (!file.name.endsWith('.zip')) {
      setUploadPhase('error');
      setUploadMessage('请上传 zip 格式的文件');
      return;
    }

    resetAll();
    setUploadPhase('uploading');
    setUploadMessage('正在处理...');
    setFallbackCount(0);

    // 创建 AbortController
    const controller = new AbortController();
    abortControllerRef.current = controller;

    // 创建 EventSource 连接来获取进度推送
    const progressSource = new EventSource('http://localhost:8000/api/notes/upload-progress');
    sseRef.current = progressSource;

    progressSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.type === 'progress') {
          const newPhase = data.phase;
          setPhase(newPhase);

          // 计算总进度
          const phaseCfg = PHASE_CONFIG[newPhase] || PHASE_CONFIG.idle;
          let totalPercent = phaseCfg.percent;

          // 如果在 generating 阶段，按 qa 进度细分 40%~90%
          if (newPhase === 'generating' && data.total > 0) {
            const qaPercent = data.current / data.total;
            totalPercent = 40 + Math.round(qaPercent * 50);
          }

          setOverallProgress(totalPercent);
          setQaProgress({
            current: data.current,
            total: data.total,
            currentFile: data.current_file || '',
          });

          setUploadMessage(phaseCfg.label);
        }
      } catch {
        // ignore parse error
      }
    };

    progressSource.onerror = () => {
      progressSource.close();
      sseRef.current = null;
    };

    try {
      const res = await notesApi.upload(file, selectedSection, controller.signal);
      progressSource.close();
      sseRef.current = null;

      const notes = res.data.notes || [];
      const created = notes.filter((n) => n.status === 'created').length;
      const updated = notes.filter((n) => n.status === 'updated').length;
      const unchanged = notes.filter((n) => n.status === 'unchanged').length;
      const fallback = res.data.fallback_count || 0;

      setUploadPhase('success');
      setPhase('done');
      setOverallProgress(100);
      let msg = `处理完成！`;
      if (created > 0) msg += ` 新建 ${created} 个笔记，`;
      if (updated > 0) msg += ` 更新 ${updated} 个笔记，`;
      if (unchanged > 0) msg += ` ${unchanged} 个未变更，`;
      msg = msg.replace(/，$/, '。');
      if (notes.length === 0) msg = '未找到 markdown 文件。';
      setUploadMessage(msg);
      setFallbackCount(fallback);

      onUploadSuccess?.();
    } catch (err) {
      progressSource.close();
      sseRef.current = null;

      if (err.name === 'AbortError' || err.message?.includes('cancel')) {
        // 已处理在 handleCancel 中
        return;
      }

      setUploadPhase('error');
      setPhase('idle');
      setOverallProgress(0);
      const detail = err.response?.data?.detail || err.message;
      if (detail.includes('zip 解压失败')) {
        setUploadMessage('解压失败: ' + detail);
      } else if (detail.includes('文件保存失败')) {
        setUploadMessage('上传失败: ' + detail);
      } else if (detail.includes('已取消')) {
        setUploadMessage('已取消上传');
      } else {
        setUploadMessage('上传失败: ' + detail);
      }
    } finally {
      setIsCancelling(false);
      abortControllerRef.current = null;
    }
  };

  const getPhaseDisplay = () => {
    switch (uploadPhase) {
      case 'uploading':
        return { icon: '🤖', text: uploadMessage, color: 'text-blue-600' };
      case 'success':
        return { icon: '✅', text: uploadMessage, color: 'text-green-600' };
      case 'error':
        return { icon: '❌', text: uploadMessage, color: 'text-red-600' };
      default:
        return { icon: '📁', text: selectedSection ? `上传到「${selectedSection}」板块` : '点击或拖拽上传 zip 文件', color: 'text-gray-600' };
    }
  };

  const phaseDisplay = getPhaseDisplay();
  const isProcessing = uploadPhase === 'uploading';
  const qaPercent = qaProgress.total > 0 ? Math.round((qaProgress.current / qaProgress.total) * 100) : 0;

  // 流程步骤高亮
  const steps = [
    { key: 'uploading', label: '上传' },
    { key: 'extracting', label: '解压' },
    { key: 'parsing', label: '解析' },
    { key: 'generating', label: 'LLM生成QA' },
    { key: 'writing', label: '写入数据库' },
  ];

  const getStepStatus = (stepKey) => {
    const stepOrder = ['uploading', 'extracting', 'parsing', 'generating', 'writing', 'done'];
    const currentIdx = stepOrder.indexOf(phase);
    const stepIdx = stepOrder.indexOf(stepKey);
    if (stepIdx < currentIdx) return 'done';
    if (stepIdx === currentIdx) return 'active';
    return 'pending';
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-2">上传笔记</h3>
      <p className="text-sm text-gray-500 mb-4">
        {selectedSection
          ? `笔记将上传到「${selectedSection}」板块`
          : '将 Obsidian 笔记文件夹打包为 zip 文件后上传，会自动按文件夹名识别板块'}
      </p>

      {/* 上传区域 */}
      <div
        className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors ${
          dragActive
            ? 'border-blue-500 bg-blue-50'
            : uploadPhase === 'error'
            ? 'border-red-300 bg-red-50'
            : uploadPhase === 'success'
            ? 'border-green-300 bg-green-50'
            : 'border-gray-300 hover:border-gray-400'
        } ${!isProcessing ? 'cursor-pointer' : ''}`}
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => !isProcessing && inputRef.current?.click()}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".zip"
          className="hidden"
          onChange={handleChange}
          disabled={isProcessing}
        />
        <div className="text-3xl mb-2">{phaseDisplay.icon}</div>
        <p className={`text-sm ${phaseDisplay.color} font-medium`}>{phaseDisplay.text}</p>

        {/* 总进度条 */}
        {isProcessing && (
          <div className="mt-4">
            {/* 流程步骤指示器 */}
            <div className="flex items-center justify-center gap-1 mb-3 text-xs">
              {steps.map((step, idx) => {
                const status = getStepStatus(step.key);
                return (
                  <div key={step.key} className="flex items-center">
                    <span
                      className={`px-2 py-0.5 rounded ${
                        status === 'done'
                          ? 'bg-green-100 text-green-700'
                          : status === 'active'
                          ? 'bg-blue-100 text-blue-700 font-medium'
                          : 'text-gray-400'
                      }`}
                    >
                      {status === 'done' ? '✓' : ''} {step.label}
                    </span>
                    {idx < steps.length - 1 && (
                      <span className="mx-1 text-gray-300">→</span>
                    )}
                  </div>
                );
              })}
            </div>

            {/* 总进度条 */}
            <div className="w-full bg-gray-200 rounded-full h-3">
              <div
                className="bg-blue-600 h-3 rounded-full transition-all duration-500 ease-out"
                style={{ width: `${overallProgress}%` }}
              />
            </div>
            <div className="flex justify-between items-center mt-1">
              <p className="text-xs text-gray-500">{PHASE_CONFIG[phase]?.label || ''}</p>
              <p className="text-xs text-gray-500 font-medium">{overallProgress}%</p>
            </div>

            {/* 子进度条：仅在 generating 阶段显示 */}
            {phase === 'generating' && qaProgress.total > 0 && (
              <div className="mt-3 p-3 bg-gray-50 rounded-lg">
                <div className="flex justify-between items-center mb-1">
                  <p className="text-xs text-gray-600 font-medium">QA 生成进度</p>
                  <p className="text-xs text-gray-500">
                    {qaProgress.current} / {qaProgress.total}
                  </p>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-1.5">
                  <div
                    className="bg-emerald-500 h-1.5 rounded-full transition-all duration-300"
                    style={{ width: `${qaPercent}%` }}
                  />
                </div>
                {qaProgress.currentFile && (
                  <p className="text-xs text-gray-400 mt-1 truncate" title={qaProgress.currentFile}>
                    {qaProgress.currentFile}
                  </p>
                )}
              </div>
            )}

            {/* 取消按钮 */}
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleCancel();
              }}
              disabled={isCancelling}
              className="mt-3 px-4 py-1.5 text-sm text-red-600 bg-red-50 hover:bg-red-100 rounded-lg transition-colors disabled:opacity-50"
            >
              {isCancelling ? '取消中...' : '取消上传'}
            </button>
          </div>
        )}
      </div>

      {/* 上传结果提示 */}
      {uploadPhase === 'success' && fallbackCount > 0 && (
        <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-sm text-yellow-700">
          <span className="font-medium">⚠️ 提示：</span>
          有 {fallbackCount} 个复习项因 LLM 调用失败，已使用本地规则自动生成简单问题。
          建议检查 LLM 配置（设置 → 测试连接）。
        </div>
      )}
    </div>
  );
}
