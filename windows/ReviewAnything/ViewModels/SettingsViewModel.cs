using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class SettingsViewModel : INotifyPropertyChanged
{
    private readonly AppDbContext _db = new();

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

    private string _modelName = "deepseek-v4-pro";
    public string ModelName
    {
        get => _modelName;
        set { _modelName = value; OnPropertyChanged(); }
    }

    private int _dailyCount = 10;
    public int DailyCount
    {
        get => _dailyCount;
        set { _dailyCount = value; OnPropertyChanged(); }
    }

    public ICommand SaveCommand { get; }

    public SettingsViewModel()
    {
        LoadConfig();
        SaveCommand = new RelayCommand(async () => await SaveAsync());
    }

    private void LoadConfig()
    {
        var config = _db.Configs.FirstOrDefault();
        if (config != null)
        {
            ApiBaseUrl = config.ApiBaseUrl ?? "";
            ApiKey = config.ApiKey ?? "";
            ModelName = config.ModelName;
            DailyCount = config.DailyReviewCount;
        }
    }

    private async Task SaveAsync()
    {
        var config = _db.Configs.FirstOrDefault() ?? new Config();
        config.ApiBaseUrl = string.IsNullOrWhiteSpace(ApiBaseUrl) ? null : ApiBaseUrl;
        config.ApiKey = string.IsNullOrWhiteSpace(ApiKey) ? null : ApiKey;
        config.ModelName = ModelName;
        config.DailyReviewCount = DailyCount;

        if (config.Id == 0)
            _db.Configs.Add(config);

        await _db.SaveChangesAsync();
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
