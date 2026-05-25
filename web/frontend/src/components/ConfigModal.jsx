import { useState, useEffect } from 'react';
import { configApi } from '../api';

const LLM_PROVIDERS = [
  {
    key: 'openai',
    name: 'OpenAI',
    apiBaseUrl: 'https://api.openai.com/v1/chat/completions',
    modelName: 'gpt-3.5-turbo',
    guide: [
      '1. 访问 https://platform.openai.com/api-keys',
      '2. 登录后点击 "Create new secret key"',
      '3. 复制生成的 sk- 开头的密钥',
      '4. 在官网文档中查找 API Base URL 和模型名称',
    ],
  },
  {
    key: 'kimi',
    name: 'Kimi (Moonshot)',
    apiBaseUrl: 'https://api.moonshot.cn/v1/chat/completions',
    modelName: 'moonshot-v1-8k',
    guide: [
      '1. 访问 https://platform.moonshot.cn/',
      '2. 注册/登录后进入「API Key 管理」',
      '3. 点击「新建」生成 API Key',
      '4. 复制密钥填入上方 API Key 栏',
      '5. 在官网文档中查找 API Base URL 和模型名称',
    ],
  },
  {
    key: 'deepseek',
    name: 'DeepSeek',
    apiBaseUrl: 'https://api.deepseek.com/chat/completions',
    modelName: 'deepseek-chat',
    guide: [
      '1. 访问 https://platform.deepseek.com/',
      '2. 注册/登录后进入「API Keys」',
      '3. 点击「创建 API Key」',
      '4. 复制密钥填入上方 API Key 栏',
      '5. 在官网文档中查找 API Base URL 和模型名称',
    ],
  },
  {
    key: 'minimax',
    name: 'MiniMax',
    apiBaseUrl: 'https://api.minimax.chat/v1/chat/completions',
    modelName: 'abab6.5s-chat',
    guide: [
      '1. 访问 https://platform.minimaxi.com/',
      '2. 注册/登录后进入「开发者中心」→「API 密钥」',
      '3. 创建新的 API Key',
      '4. 复制 Group ID 和 API Key 填入',
      '5. 在官网文档中查找 API Base URL 和模型名称',
    ],
  },
  {
    key: 'custom',
    name: '自定义',
    apiBaseUrl: '',
    modelName: '',
    guide: [
      '1. 输入你的 API Base URL',
      '2. 输入你的 API Key',
      '3. 输入模型名称',
      '4. 点击「测试连接」验证',
    ],
  },
];

export default function ConfigModal({ onConfigured }) {
  const [isOpen, setIsOpen] = useState(false);
  const [form, setForm] = useState({
    api_base_url: '',
    api_key: '',
    model_name: '',
    daily_review_count: 10,
  });
  const [selectedProvider, setSelectedProvider] = useState('openai');
  const [loading, setLoading] = useState(false);
  const [testStatus, setTestStatus] = useState(null);

  useEffect(() => {
    checkConfig();
  }, []);

  const checkConfig = async () => {
    try {
      const res = await configApi.get();
      if (!res.data.configured) {
        setIsOpen(true);
      } else {
        onConfigured?.(res.data.has_llm);
      }
    } catch (e) {
      setIsOpen(true);
    }
  };

  const handleProviderChange = (key) => {
    setSelectedProvider(key);
    const provider = LLM_PROVIDERS.find((p) => p.key === key);
    if (provider && key !== 'custom') {
      setForm((prev) => ({
        ...prev,
        api_base_url: provider.apiBaseUrl,
        model_name: provider.modelName,
      }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await configApi.save(form);
      setIsOpen(false);
      // 询问是否更新 QA
      const shouldUpdate = window.confirm(
        '配置已保存！是否使用新模型重新生成所有 QA？\n\n点击「确定」更新全部 QA\n点击「取消」稍后手动更新'
      );
      onConfigured?.(true, shouldUpdate);
    } catch (err) {
      alert('保存失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleSkip = async () => {
    setLoading(true);
    try {
      // 保存一个仅包含每日复习数量的空配置
      await configApi.save({
        api_base_url: '',
        api_key: '',
        model_name: 'gpt-3.5-turbo',
        daily_review_count: form.daily_review_count,
      });
      setIsOpen(false);
      onConfigured?.(false);
    } catch (err) {
      alert('保存失败: ' + (err.response?.data?.detail || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleTestConnection = async () => {
    if (!form.api_base_url || !form.api_key) {
      setTestStatus({ success: false, message: '请先填写 API Base URL 和 API Key' });
      return;
    }
    setTestStatus('testing');
    try {
      const res = await configApi.test(form);
      setTestStatus(res.data);
    } catch (err) {
      setTestStatus({ success: false, message: '测试请求失败: ' + (err.response?.data?.detail || err.message) });
    }
  };

  if (!isOpen) return null;

  const currentProvider = LLM_PROVIDERS.find((p) => p.key === selectedProvider);

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-6 m-4 max-h-[90vh] overflow-y-auto">
        <h2 className="text-xl font-bold text-gray-900 mb-2">首次配置</h2>
        <p className="text-sm text-gray-500 mb-4">
          可选择配置 LLM 获得更好的 QA 生成效果，或点击「暂不配置」先使用本地规则
        </p>

        {/* LLM 提供商选择 */}
        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            选择 LLM 提供商
          </label>
          <div className="grid grid-cols-3 gap-2">
            {LLM_PROVIDERS.map((provider) => (
              <button
                key={provider.key}
                type="button"
                onClick={() => handleProviderChange(provider.key)}
                className={`py-2 px-1 rounded-lg text-sm font-medium border transition-colors ${
                  selectedProvider === provider.key
                    ? 'bg-blue-600 text-white border-blue-600'
                    : 'bg-white text-gray-700 border-gray-200 hover:border-blue-300'
                }`}
              >
                {provider.name}
              </button>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              API Base URL
            </label>
            <input
              type="url"
              required
              placeholder="https://api.openai.com/v1"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              value={form.api_base_url}
              onChange={(e) => setForm({ ...form, api_base_url: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              API Key
            </label>
            <input
              type="password"
              required
              placeholder="sk-..."
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              value={form.api_key}
              onChange={(e) => setForm({ ...form, api_key: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              模型名称
            </label>
            <input
              type="text"
              required
              placeholder="gpt-3.5-turbo"
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              value={form.model_name}
              onChange={(e) => setForm({ ...form, model_name: e.target.value })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              每日复习数量
            </label>
            <input
              type="number"
              min={1}
              max={100}
              required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
              value={form.daily_review_count}
              onChange={(e) => setForm({ ...form, daily_review_count: parseInt(e.target.value) || 10 })}
            />
          </div>

          {/* 测试连接按钮 */}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleTestConnection}
              disabled={testStatus === 'testing'}
              className="flex-1 py-2 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              {testStatus === 'testing' ? '测试中...' : '测试连接'}
            </button>
          </div>

          {/* 测试结果提示 */}
          {testStatus && testStatus !== 'testing' && (
            <div
              className={`p-3 rounded-lg text-sm ${
                testStatus.success
                  ? 'bg-green-50 text-green-700 border border-green-200'
                  : 'bg-red-50 text-red-700 border border-red-200'
              }`}
            >
              <div className="flex items-center gap-2">
                <span className="text-lg">{testStatus.success ? '✅' : '❌'}</span>
                <span>{testStatus.message}</span>
              </div>
            </div>
          )}

          {/* 申请指南 */}
          {currentProvider && (
            <div className="bg-blue-50 rounded-lg p-4 border border-blue-100">
              <h4 className="text-sm font-semibold text-blue-800 mb-2">
                📖 {currentProvider.name} API 申请指南
              </h4>
              <ul className="space-y-1">
                {currentProvider.guide.map((step, idx) => (
                  <li key={idx} className="text-sm text-blue-700">
                    {step}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? '保存中...' : '保存配置'}
            </button>
            <button
              type="button"
              onClick={handleSkip}
              disabled={loading}
              className="flex-1 py-2.5 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              暂不配置
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
