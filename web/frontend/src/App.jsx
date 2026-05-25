import { useState, useCallback, useEffect } from 'react';
import ConfigModal from './components/ConfigModal';
import Sidebar from './components/Sidebar';
import UploadPanel from './components/UploadPanel';
import ReviewCard from './components/ReviewCard';
import NotesManager from './components/NotesManager';
import SettingsPanel from './components/SettingsPanel';
import SectionManager from './components/SectionManager';
import CheckInCalendar from './components/CheckInCalendar';
import { reviewApi, notesApi, configApi, checkinApi } from './api';

function App() {
  const [configured, setConfigured] = useState(false);
  const [hasLLM, setHasLLM] = useState(false);
  const [selectedSection, setSelectedSection] = useState(null);
  const [activeTab, setActiveTab] = useState('review'); // 'review' | 'manage' | 'settings'
  const [refreshKey, setRefreshKey] = useState(0);

  // 复习流程状态
  const [reviewMode, setReviewMode] = useState('idle'); // 'idle' | 'reviewing' | 'finished'
  const [reviewQueue, setReviewQueue] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [dailyCount, setDailyCount] = useState(10);
  const [reviewSection, setReviewSection] = useState(null); // 复习时选中的板块
  const [sections, setSections] = useState([]);
  const [sessionStats, setSessionStats] = useState({ total: 0, forget: 0 });

  // 打卡状态
  const [checkinStreak, setCheckinStreak] = useState(0);
  const [checkedToday, setCheckedToday] = useState(false);

  // 加载配置和板块列表
  useEffect(() => {
    if (configured) {
      loadConfig();
      loadSections();
      loadCheckinStatus();
    }
  }, [configured]);

  const loadConfig = async () => {
    try {
      const res = await configApi.get();
      if (res.data.configured && res.data.config.daily_review_count) {
        setDailyCount(res.data.config.daily_review_count);
      }
    } catch (e) {
      console.error('加载配置失败', e);
    }
  };

  const loadSections = async () => {
    try {
      const res = await notesApi.getSections();
      setSections(res.data);
    } catch (e) {
      console.error('加载板块失败', e);
    }
  };

  const handleConfigured = (hasLLMConfig = false, shouldUpdateQA = false) => {
    setConfigured(true);
    setHasLLM(hasLLMConfig);
    if (shouldUpdateQA) {
      handleRegenerateQA();
    }
  };

  const handleRegenerateQA = async (section = null) => {
    const target = section || '全部';
    if (!confirm(`确定要使用当前模型重新生成「${target}」板块的 QA 吗？\n\n这会删除现有的 QA 并重新生成，复习进度将保留。`)) {
      return;
    }
    try {
      const res = await notesApi.regenerateQA(section);
      alert(`QA 更新完成！\n更新了 ${res.data.updated_notes} 个笔记，${res.data.updated_chunks} 个分块`);
      setRefreshKey((k) => k + 1);
    } catch (err) {
      alert('更新失败: ' + (err.response?.data?.detail || err.message));
    }
  };

  const loadCheckinStatus = async () => {
    try {
      const res = await checkinApi.getStatus();
      setCheckinStreak(res.data.streak);
      setCheckedToday(res.data.checked_today);
    } catch (e) {
      console.error('加载打卡状态失败', e);
    }
  };

  const handleConfigDeleted = () => {
    setConfigured(false);
    window.location.reload();
  };

  const handleSectionChange = (section) => {
    setSelectedSection(section);
    setRefreshKey((k) => k + 1);
  };

  const handleUploadSuccess = () => {
    setRefreshKey((k) => k + 1);
    loadSections();
  };

  // 开始复习
  const startReview = async () => {
    const count = dailyCount;
    const section = reviewSection;

    try {
      const res = await reviewApi.getBatch(section, count);
      const items = res.data.items || [];
      if (items.length === 0) {
        alert('该范围内没有待复习的内容');
        return;
      }
      setReviewQueue(items);
      setCurrentIndex(0);
      setSessionStats({ total: items.length, forget: 0 });
      setReviewMode('reviewing');
    } catch (e) {
      console.error('加载复习项失败', e);
      alert('加载复习内容失败');
    }
  };

  // 处理复习动作
  const handleReviewAction = useCallback(
    (action) => {
      if (action === 'forget') {
        setSessionStats((s) => ({ ...s, forget: s.forget + 1 }));
      }

      const nextIndex = currentIndex + 1;
      if (nextIndex >= reviewQueue.length) {
        setReviewMode('finished');
      } else {
        setCurrentIndex(nextIndex);
      }
    },
    [currentIndex, reviewQueue.length]
  );

  // 再来一组
  const handleRestart = () => {
    setReviewMode('idle');
    setReviewQueue([]);
    setCurrentIndex(0);
    setSessionStats({ total: 0, forget: 0 });
  };

  // 渲染复习空闲状态
  const renderReviewIdle = () => (
    <div className="max-w-xl mx-auto space-y-8">
      {/* 打卡提示 */}
      <div className={`rounded-xl p-4 flex items-center justify-between ${
        checkedToday ? 'bg-green-50 border border-green-200' : 'bg-orange-50 border border-orange-200'
      }`}>
        <div>
          <p className={`text-sm font-medium ${checkedToday ? 'text-green-800' : 'text-orange-800'}`}>
            {checkedToday ? '✅ 今日已打卡' : '⏰ 今日还未打卡'}
          </p>
          <p className={`text-xs mt-0.5 ${checkedToday ? 'text-green-600' : 'text-orange-600'}`}>
            已连续打卡 <span className="font-bold text-base">{checkinStreak}</span> 天
            {checkinStreak > 0 ? ' 🔥 加油！' : ' 💪 开始你的第一天！'}
          </p>
        </div>
      </div>

      <div className="text-center">
        <div className="text-5xl mb-4">📚</div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">准备开始复习</h2>
        <p className="text-gray-500">选择复习范围，然后开始今日复习计划</p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
        {/* 板块选择 */}
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-3">
            选择复习范围
          </label>
          <div className="space-y-2">
            <label className="flex items-center gap-3 p-3 rounded-lg border border-gray-200 cursor-pointer hover:bg-gray-50 transition-colors">
              <input
                type="radio"
                name="review-section"
                checked={reviewSection === null}
                onChange={() => setReviewSection(null)}
                className="w-4 h-4 text-blue-600"
              />
              <span className="text-sm font-medium text-gray-900">全部板块</span>
              <span className="text-xs text-gray-400 ml-auto">复习所有内容</span>
            </label>

            {sections.map((section) => (
              <label
                key={section}
                className="flex items-center gap-3 p-3 rounded-lg border border-gray-200 cursor-pointer hover:bg-gray-50 transition-colors"
              >
                <input
                  type="radio"
                  name="review-section"
                  checked={reviewSection === section}
                  onChange={() => setReviewSection(section)}
                  className="w-4 h-4 text-blue-600"
                />
                <span className="text-sm font-medium text-gray-900">{section}</span>
              </label>
            ))}

            <label className="flex items-center gap-3 p-3 rounded-lg border border-gray-200 cursor-pointer hover:bg-gray-50 transition-colors">
              <input
                type="radio"
                name="review-section"
                checked={reviewSection === '__random__'}
                onChange={() => setReviewSection('__random__')}
                className="w-4 h-4 text-blue-600"
              />
              <span className="text-sm font-medium text-gray-900">🎲 随机复习</span>
              <span className="text-xs text-gray-400 ml-auto">随机选择一个板块</span>
            </label>
          </div>
        </div>

        {/* 每日数量 */}
        <div className="flex items-center justify-between py-3 border-t border-gray-100">
          <span className="text-sm text-gray-600">今日计划复习</span>
          <span className="text-lg font-bold text-blue-600">{dailyCount} 题</span>
        </div>

        {/* 开始按钮 */}
        <button
          onClick={startReview}
          className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors text-lg"
        >
          开始复习
        </button>
      </div>

      {/* 使用说明 */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-bold text-gray-900 mb-4">📖 使用说明</h3>
        <div className="space-y-4">
          <div className="flex gap-4">
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm shrink-0">1</div>
            <div>
              <p className="font-medium text-gray-900">配置 LLM（可选）</p>
              <p className="text-sm text-gray-500 mt-0.5">首次打开会弹出配置窗口，填写 DeepSeek / OpenAI 等 API 信息。也可点击「暂不配置」使用本地规则生成复习卡片。</p>
            </div>
          </div>
          <div className="flex gap-4">
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm shrink-0">2</div>
            <div>
              <p className="font-medium text-gray-900">上传笔记</p>
              <p className="text-sm text-gray-500 mt-0.5">切换到「笔记管理」页，拖拽或选择 ZIP 文件上传。支持 Markdown 格式，会自动按标题分块并生成 QA 复习卡片。</p>
            </div>
          </div>
          <div className="flex gap-4">
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm shrink-0">3</div>
            <div>
              <p className="font-medium text-gray-900">开始复习</p>
              <p className="text-sm text-gray-500 mt-0.5">回到「每日复习」页，选择复习范围后点击「开始复习」。系统按间隔重复算法调度，记住/忘记决定下次复习时间。</p>
            </div>
          </div>
          <div className="flex gap-4">
            <div className="w-8 h-8 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-sm shrink-0">4</div>
            <div>
              <p className="font-medium text-gray-900">坚持打卡</p>
              <p className="text-sm text-gray-500 mt-0.5">左侧边栏可以打卡，日历会记录你的复习坚持天数。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // 渲染复习中状态
  const renderReviewing = () => {
    const currentItem = reviewQueue[currentIndex];
    return (
      <div className="max-w-2xl mx-auto space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-bold text-gray-900">每日复习</h2>
          <button
            onClick={handleRestart}
            className="px-3 py-1.5 text-sm text-gray-500 hover:text-gray-700 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
          >
            结束复习
          </button>
        </div>
        <ReviewCard
          item={currentItem}
          onAction={handleReviewAction}
          currentIndex={currentIndex}
          totalCount={reviewQueue.length}
        />
      </div>
    );
  };

  // 渲染完成状态
  const renderFinished = () => (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
        <div className="text-6xl mb-4">🎉</div>
        <h3 className="text-2xl font-bold text-gray-900 mb-2">今日复习完成！</h3>
        <p className="text-gray-500 mb-6">
          已完成 {sessionStats.total} 题复习
        </p>

        <div className="grid grid-cols-2 gap-4 mb-6">
          <div className="bg-blue-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-blue-600">{sessionStats.total}</div>
            <div className="text-sm text-gray-500">总题数</div>
          </div>
          <div className="bg-red-50 rounded-lg p-4">
            <div className="text-2xl font-bold text-red-600">{sessionStats.forget}</div>
            <div className="text-sm text-gray-500">易忘标记</div>
          </div>
        </div>

        <button
          onClick={handleRestart}
          className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
        >
          再来一组
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex h-screen bg-gray-50">
      <ConfigModal onConfigured={handleConfigured} />
      <Sidebar
        selectedSection={selectedSection}
        onSelectSection={handleSectionChange}
        key={refreshKey}
      />

      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部导航 */}
        <div className="bg-white border-b border-gray-200 px-6 py-3">
          <div className="flex items-center gap-1 bg-gray-100 p-1 rounded-lg inline-flex">
            <button
              onClick={() => {
                setActiveTab('review');
                if (reviewMode !== 'reviewing') {
                  setReviewMode('idle');
                }
              }}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'review'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              每日复习
            </button>
            <button
              onClick={() => setActiveTab('manage')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'manage'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              笔记管理
            </button>
            <button
              onClick={() => setActiveTab('settings')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'settings'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              设置
            </button>
          </div>
        </div>

        {/* 主内容区 */}
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === 'review' && (
            <>
              {reviewMode === 'idle' && renderReviewIdle()}
              {reviewMode === 'reviewing' && renderReviewing()}
              {reviewMode === 'finished' && renderFinished()}
            </>
          )}

          {activeTab === 'manage' && (
            <div className="max-w-2xl mx-auto space-y-6">
              <h2 className="text-xl font-bold text-gray-900">笔记管理</h2>
              <SectionManager
                selectedSection={selectedSection}
                onSelectSection={handleSectionChange}
                onSectionsChanged={() => setRefreshKey((k) => k + 1)}
              />
              <UploadPanel onUploadSuccess={handleUploadSuccess} selectedSection={selectedSection} />
              <NotesManager
                selectedSection={selectedSection}
                onNotesChanged={() => setRefreshKey((k) => k + 1)}
              />
            </div>
          )}

          {activeTab === 'settings' && (
            <div className="max-w-2xl mx-auto space-y-6">
              <h2 className="text-xl font-bold text-gray-900">设置</h2>
              <SettingsPanel
                onConfigDeleted={handleConfigDeleted}
                dailyCount={dailyCount}
                onDailyCountChange={setDailyCount}
                onRegenerateQA={handleRegenerateQA}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
