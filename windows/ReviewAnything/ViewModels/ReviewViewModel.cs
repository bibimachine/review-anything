using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Microsoft.EntityFrameworkCore;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class ReviewViewModel : INotifyPropertyChanged
{
    private ReviewService ReviewSvc => _reviewSvc ??= new ReviewService(new AppDbContext());
    private ReviewService? _reviewSvc;
    private AppDbContext Db => _db ??= new AppDbContext();
    private AppDbContext? _db;

    public ObservableCollection<ReviewItem> Items { get; } = new();

    private int _currentIndex;
    public int CurrentIndex
    {
        get => _currentIndex;
        set { _currentIndex = value; OnPropertyChanged(); OnPropertyChanged(nameof(CurrentItem)); OnPropertyChanged(nameof(ProgressText)); OnPropertyChanged(nameof(IsFinished)); }
    }

    private bool _showAnswer;
    public bool ShowAnswer
    {
        get => _showAnswer;
        set { _showAnswer = value; OnPropertyChanged(); }
    }

    private bool _isEmpty;
    public bool IsEmpty
    {
        get => _isEmpty;
        set { _isEmpty = value; OnPropertyChanged(); OnPropertyChanged(nameof(IsIdle)); }
    }

    // 打卡状态
    private int _streak;
    public int Streak
    {
        get => _streak;
        set { _streak = value; OnPropertyChanged(); }
    }

    private bool _checkedToday;
    public bool CheckedToday
    {
        get => _checkedToday;
        set { _checkedToday = value; OnPropertyChanged(); }
    }

    public ReviewItem? CurrentItem => Items.Count > CurrentIndex ? Items[CurrentIndex] : null;
    public string ProgressText => Items.Count > 0 ? $"{CurrentIndex + 1} / {Items.Count}" : "";
    public bool IsFinished => Items.Count > 0 && CurrentIndex >= Items.Count;
    public bool IsIdle => Items.Count == 0 && !IsEmpty;

    public ICommand StartCommand { get; }
    public ICommand ShowAnswerCommand { get; }
    public ICommand RememberCommand { get; }
    public ICommand ForgetCommand { get; }
    public ICommand RestartCommand { get; }

    public ReviewViewModel()
    {
        StartCommand = new AsyncRelayCommand(LoadItemsAsync);
        ShowAnswerCommand = new RelayCommand(() => ShowAnswer = true);
        RememberCommand = new AsyncRelayCommand(() => MarkAsync(true));
        ForgetCommand = new AsyncRelayCommand(() => MarkAsync(false));
        RestartCommand = new RelayCommand(() => { CurrentIndex = 0; ShowAnswer = false; IsEmpty = false; Items.Clear(); });
        _ = LoadCheckInStatusAsync();
    }

    private async Task LoadCheckInStatusAsync()
    {
        try
        {
            var dates = await Task.Run(() => Db.CheckIns.Select(c => c.CheckinDate).ToList());
            CheckedToday = dates.Contains(DateTime.Now.ToString("yyyy-MM-dd"));
            Streak = CalculateStreak(dates.ToHashSet());
        }
        catch
        {
            CheckedToday = false;
            Streak = 0;
        }
    }

    private async Task LoadItemsAsync()
    {
        Items.Clear();
        IsEmpty = false;
        try
        {
            var items = await ReviewSvc.GetDueItemsAsync(10);
            foreach (var item in items) Items.Add(item);
            IsEmpty = items.Count == 0;
        }
        catch
        {
            IsEmpty = true;
        }
        CurrentIndex = 0;
        ShowAnswer = false;
        _ = LoadCheckInStatusAsync();
    }

    private async Task MarkAsync(bool remembered)
    {
        try
        {
            var item = CurrentItem;
            if (item == null) return;

            if (remembered)
                await ReviewSvc.MarkRememberedAsync(item);
            else
                await ReviewSvc.MarkForgottenAsync(item);

            ShowAnswer = false;
            CurrentIndex++;

            // 复习完成自动打卡
            if (IsFinished)
            {
                await AutoCheckInAsync();
            }
        }
        catch
        {
            // ignore db errors during review
        }
    }

    private async Task AutoCheckInAsync()
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        if (!await Db.CheckIns.AnyAsync(c => c.CheckinDate == today))
        {
            Db.CheckIns.Add(new CheckIn { CheckinDate = today });
            await Db.SaveChangesAsync();
            await LoadCheckInStatusAsync();
        }
    }

    private static int CalculateStreak(HashSet<string> dates)
    {
        if (dates.Count == 0) return 0;
        var streak = 0;
        var current = DateTime.Now;
        while (true)
        {
            if (dates.Contains(current.ToString("yyyy-MM-dd")))
            {
                streak++;
                current = current.AddDays(-1);
            }
            else break;
        }
        return streak;
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
