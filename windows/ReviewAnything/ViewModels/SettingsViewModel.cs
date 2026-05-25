using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public record LlmProvider(string Key, string Name, string ApiBaseUrl, string ModelName, string[] Guide);

public class SettingsViewModel : INotifyPropertyChanged
{
    public LlmProvider[] Providers { get; } = new[]
    {
        new LlmProvider("deepseek", "DeepSeek", "https://api.deepseek.com/chat/completions", "deepseek-chat", new[]
        {
            "1. 访问 https://platform.deepseek.com/",
            "2. 注册/登录后进入「API Keys」",
            "3. 点击「创建 API Key」",
            "4. 复制密钥填入上方 API Key 栏"
        }),
        new LlmProvider("openai", "OpenAI", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo", new[]
        {
            "1. 访问 https://platform.openai.com/api-keys",
            "2. 登录后点击 Create new secret key",
            "3. 复制 sk- 开头的密钥填入上方"
        }),
        new LlmProvider("kimi", "Kimi", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k", new[]
        {
            "1. 访问 https://platform.moonshot.cn/",
            "2. 进入「API Key 管理」",
            "3. 点击「新建」生成 API Key"
        }),
        new LlmProvider("custom", "自定义", "", "", new[]
        {
            "1. 输入你的 API Base URL",
            "2. 输入 API Key",
            "3. 输入模型名称"
        })
    };

    private LlmProvider _selectedProvider;
    public LlmProvider SelectedProvider
    {
        get => _selectedProvider;
        set
        {
            _selectedProvider = value;
            OnPropertyChanged();
            OnPropertyChanged(nameof(GuideText));
            if (value.Key != "custom")
            {
                ApiBaseUrl = value.ApiBaseUrl;
                ModelName = value.ModelName;
            }
        }
    }

    public string GuideText => string.Join("\n", SelectedProvider.Guide);

    private string _apiBaseUrl = "";
    public string ApiBaseUrl
    {
        get => _apiBaseUrl;
        set { _apiBaseUrl = value; OnPropertyChanged(); }
    }

    private string _apiKey = "";
    public string ApiKey
    {
        get => _apiKey;
        set { _apiKey = value; OnPropertyChanged(); }
    }

    private string _modelName = "deepseek-chat";
    public string ModelName
    {
        get => _modelName;
        set { _modelName = value; OnPropertyChanged(); }
    }

    private int _dailyCount = 10;
    public string DailyCount
    {
        get => _dailyCount.ToString();
        set { if (int.TryParse(value, out var n)) { _dailyCount = n; OnPropertyChanged(); } }
    }

    private string? _testResult;
    public string? TestResult
    {
        get => _testResult;
        set { _testResult = value; OnPropertyChanged(); }
    }

    public ICommand SaveCommand { get; }
    public ICommand TestCommand { get; }
    public ICommand SkipCommand { get; }

    public SettingsViewModel()
    {
        _selectedProvider = Providers[0];
        LoadConfig();
        SaveCommand = new RelayCommand(async () => await SaveAsync());
        TestCommand = new RelayCommand(() => TestConnection());
        SkipCommand = new RelayCommand(async () => await SkipAsync());
    }

    private void LoadConfig()
    {
        using var db = new AppDbContext();
        var config = db.Configs.FirstOrDefault();
        if (config != null)
        {
            ApiBaseUrl = config.ApiBaseUrl ?? "";
            ApiKey = config.ApiKey ?? "";
            ModelName = config.ModelName ?? "deepseek-chat";
            _dailyCount = config.DailyReviewCount;
            OnPropertyChanged(nameof(DailyCount));
        }
    }

    private async Task SaveAsync()
    {
        using var db = new AppDbContext();
        var config = db.Configs.FirstOrDefault() ?? new Config();
        config.ApiBaseUrl = string.IsNullOrWhiteSpace(ApiBaseUrl) ? null : ApiBaseUrl;
        config.ApiKey = string.IsNullOrWhiteSpace(ApiKey) ? null : ApiKey;
        config.ModelName = ModelName;
        config.DailyReviewCount = _dailyCount;
        if (config.Id == 0) db.Configs.Add(config);
        await db.SaveChangesAsync();
        TestResult = "✅ 配置已保存";
    }

    private async Task SkipAsync()
    {
        using var db = new AppDbContext();
        var config = db.Configs.FirstOrDefault() ?? new Config();
        config.ApiBaseUrl = null;
        config.ApiKey = null;
        config.ModelName = ModelName;
        config.DailyReviewCount = _dailyCount;
        if (config.Id == 0) db.Configs.Add(config);
        await db.SaveChangesAsync();
        TestResult = "✅ 已跳过 LLM 配置，使用本地规则";
    }

    private void TestConnection()
    {
        if (string.IsNullOrWhiteSpace(ApiBaseUrl) || string.IsNullOrWhiteSpace(ApiKey))
        {
            TestResult = "❌ 请填写 API Base URL 和 API Key";
            return;
        }
        TestResult = "✅ 配置格式正确（运行时验证）";
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
