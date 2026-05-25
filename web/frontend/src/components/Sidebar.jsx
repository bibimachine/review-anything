import { useState, useEffect } from 'react';
import { notesApi, reviewApi, checkinApi } from '../api';

export default function Sidebar({ selectedSection, onSelectSection }) {
  const [sections, setSections] = useState([]);
  const [stats, setStats] = useState({ total: 0, hard: 0, today_due: 0 });
  const [checkinStreak, setCheckinStreak] = useState(0);
  const [checkedToday, setCheckedToday] = useState(false);
  const [calendarDates, setCalendarDates] = useState(new Set());
  const [currentDate, setCurrentDate] = useState(new Date());

  const loadData = async () => {
    try {
      const [sectionsRes, statsRes, checkinRes] = await Promise.all([
        notesApi.getSections(),
        reviewApi.getStats(),
        checkinApi.getStatus(),
      ]);
      setSections(sectionsRes.data);
      setStats(statsRes.data);
      setCheckinStreak(checkinRes.data.streak);
      setCheckedToday(checkinRes.data.checked_today);
    } catch (e) {
      console.error('加载数据失败', e);
    }
  };

  const loadCalendar = async () => {
    try {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth() + 1;
      const res = await checkinApi.getCalendar(year, month);
      setCalendarDates(new Set(res.data.checked_dates));
    } catch (e) {
      console.error('加载日历失败', e);
    }
  };

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 5000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    loadCalendar();
  }, [currentDate]);

  const handleCheckIn = async () => {
    if (checkedToday) return;
    try {
      const res = await checkinApi.checkin();
      setCheckedToday(true);
      setCheckinStreak(res.data.streak);
      const todayStr = new Date().toISOString().slice(0, 10);
      setCalendarDates((prev) => new Set([...prev, todayStr]));
    } catch (e) {
      console.error('打卡失败', e);
    }
  };

  const prevMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  // 生成日历
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;
  const firstDayOfMonth = new Date(year, month - 1, 1);
  const lastDayOfMonth = new Date(year, month, 0);
  const daysInMonth = lastDayOfMonth.getDate();
  const startWeekday = firstDayOfMonth.getDay();
  const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
  const todayStr = new Date().toISOString().slice(0, 10);

  const cells = [];
  for (let i = 0; i < startWeekday; i++) {
    cells.push(<div key={`e${i}`} className="h-7" />);
  }
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    const isChecked = calendarDates.has(dateStr);
    const isToday = dateStr === todayStr;
    cells.push(
      <div
        key={dateStr}
        className={`h-7 flex items-center justify-center rounded text-xs font-medium ${
          isChecked
            ? 'bg-green-500 text-white'
            : isToday
            ? 'bg-blue-100 text-blue-700 border border-blue-300'
            : 'text-gray-600'
        }`}
      >
        {day}
      </div>
    );
  }

  return (
    <div className="w-64 bg-white border-r border-gray-200 h-screen flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <h1 className="text-xl font-bold text-gray-900">📚 每日复习</h1>
      </div>

      {/* 统计卡片 */}
      <div className="p-4 grid grid-cols-3 gap-2 text-center">
        <div className="bg-blue-50 rounded-lg p-2">
          <div className="text-lg font-bold text-blue-600">{stats.total}</div>
          <div className="text-xs text-gray-500">总条目</div>
        </div>
        <div className="bg-red-50 rounded-lg p-2">
          <div className="text-lg font-bold text-red-600">{stats.hard}</div>
          <div className="text-xs text-gray-500">易忘</div>
        </div>
        <div className="bg-green-50 rounded-lg p-2">
          <div className="text-lg font-bold text-green-600">{stats.today_due}</div>
          <div className="text-xs text-gray-500">待复习</div>
        </div>
      </div>

      {/* 打卡区域 */}
      <div className="px-4 pb-3 border-b border-gray-200">
        <div className="bg-orange-50 rounded-lg p-3">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-medium text-orange-800">
              🔥 连续打卡 <span className="font-bold text-base">{checkinStreak}</span> 天
            </span>
            <button
              onClick={handleCheckIn}
              disabled={checkedToday}
              className={`px-2.5 py-1 rounded text-xs font-medium transition-colors ${
                checkedToday
                  ? 'bg-green-100 text-green-700 cursor-default'
                  : 'bg-orange-500 text-white hover:bg-orange-600'
              }`}
            >
              {checkedToday ? '已打卡' : '打卡'}
            </button>
          </div>

          {/* 月份切换 */}
          <div className="flex items-center justify-between mb-1.5">
            <button onClick={prevMonth} className="p-0.5 rounded hover:bg-orange-100 text-orange-600">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <span className="text-xs font-medium text-orange-700">{year}年{month}月</span>
            <button onClick={nextMonth} className="p-0.5 rounded hover:bg-orange-100 text-orange-600">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>

          {/* 星期标题 */}
          <div className="grid grid-cols-7 gap-0.5 mb-0.5">
            {weekdays.map((w) => (
              <div key={w} className="h-5 flex items-center justify-center text-[10px] text-orange-400">
                {w}
              </div>
            ))}
          </div>

          {/* 日期格子 */}
          <div className="grid grid-cols-7 gap-0.5">
            {cells}
          </div>
        </div>
      </div>

      {/* 板块列表 */}
      <div className="flex-1 overflow-y-auto p-4">
        <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">
          选择板块
        </h3>
        <div className="space-y-1">
          <button
            onClick={() => onSelectSection(null)}
            className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
              selectedSection === null
                ? 'bg-blue-100 text-blue-700'
                : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            全部板块
          </button>
          {sections.map((section) => (
            <button
              key={section}
              onClick={() => onSelectSection(section)}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                selectedSection === section
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {section}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
