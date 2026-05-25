import { useState, useEffect } from 'react';
import { checkinApi } from '../api';

export default function CheckInCalendar() {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [checkedDates, setCheckedDates] = useState(new Set());
  const [streak, setStreak] = useState(0);
  const [checkedToday, setCheckedToday] = useState(false);
  const [loading, setLoading] = useState(false);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth() + 1;

  const loadCalendar = async () => {
    try {
      const [statusRes, calRes] = await Promise.all([
        checkinApi.getStatus(),
        checkinApi.getCalendar(year, month),
      ]);
      setCheckedToday(statusRes.data.checked_today);
      setStreak(statusRes.data.streak);
      setCheckedDates(new Set(calRes.data.checked_dates));
    } catch (e) {
      console.error('加载打卡数据失败', e);
    }
  };

  useEffect(() => {
    loadCalendar();
  }, [year, month]);

  const handleCheckIn = async () => {
    if (checkedToday || loading) return;
    setLoading(true);
    try {
      const res = await checkinApi.checkin();
      setCheckedToday(true);
      setStreak(res.data.streak);
      const todayStr = new Date().toISOString().slice(0, 10);
      setCheckedDates((prev) => new Set([...prev, todayStr]));
    } catch (e) {
      alert('打卡失败: ' + (e.response?.data?.detail || e.message));
    } finally {
      setLoading(false);
    }
  };

  // 生成日历数据
  const firstDayOfMonth = new Date(year, month - 1, 1);
  const lastDayOfMonth = new Date(year, month, 0);
  const daysInMonth = lastDayOfMonth.getDate();
  const startWeekday = firstDayOfMonth.getDay(); // 0=周日

  const weekdays = ['日', '一', '二', '三', '四', '五', '六'];

  const prevMonth = () => {
    setCurrentDate(new Date(year, month - 2, 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(year, month, 1));
  };

  const todayStr = new Date().toISOString().slice(0, 10);

  // 构建日历格子
  const cells = [];
  // 空白格子
  for (let i = 0; i < startWeekday; i++) {
    cells.push(<div key={`empty-${i}`} className="h-10" />);
  }
  // 日期格子
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    const isChecked = checkedDates.has(dateStr);
    const isToday = dateStr === todayStr;

    cells.push(
      <div
        key={dateStr}
        className={`h-10 flex items-center justify-center rounded-lg text-sm font-medium transition-colors ${
          isChecked
            ? 'bg-green-500 text-white'
            : isToday
            ? 'bg-blue-100 text-blue-700 border-2 border-blue-300'
            : 'text-gray-700 hover:bg-gray-100'
        }`}
        title={isChecked ? '已打卡' : isToday ? '今天' : ''}
      >
        {day}
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">📅 打卡日历</h3>
        <button
          onClick={handleCheckIn}
          disabled={checkedToday || loading}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
            checkedToday
              ? 'bg-green-100 text-green-700 cursor-default'
              : 'bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50'
          }`}
        >
          {checkedToday ? '✅ 今日已打卡' : loading ? '打卡中...' : '🔥 今日打卡'}
        </button>
      </div>

      {/* 连续打卡提示 */}
      <div className="mb-4 p-3 bg-orange-50 border border-orange-200 rounded-lg">
        <p className="text-sm text-orange-800">
          <span className="font-bold text-lg">{streak}</span> 天连续打卡
          {streak > 0 ? ' 🔥 继续保持！' : ' 💪 开始你的第一天！'}
        </p>
      </div>

      {/* 月份切换 */}
      <div className="flex items-center justify-between mb-3">
        <button
          onClick={prevMonth}
          className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <span className="text-sm font-semibold text-gray-900">
          {year}年{month}月
        </span>
        <button
          onClick={nextMonth}
          className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>
      </div>

      {/* 星期标题 */}
      <div className="grid grid-cols-7 gap-1 mb-1">
        {weekdays.map((w) => (
          <div key={w} className="h-8 flex items-center justify-center text-xs text-gray-400 font-medium">
            {w}
          </div>
        ))}
      </div>

      {/* 日期格子 */}
      <div className="grid grid-cols-7 gap-1">
        {cells}
      </div>
    </div>
  );
}
