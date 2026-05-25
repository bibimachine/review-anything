using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class ReviewViewModel : INotifyPropertyChanged
{
    private readonly ReviewService _reviewService = new(new AppDbContext());

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
        StartCommand = new RelayCommand(async () => await LoadItemsAsync());
        ShowAnswerCommand = new RelayCommand(() => ShowAnswer = true);
        RememberCommand = new RelayCommand(async () => await MarkAsync(true));
        ForgetCommand = new RelayCommand(async () => await MarkAsync(false));
        RestartCommand = new RelayCommand(() => { CurrentIndex = 0; ShowAnswer = false; IsEmpty = false; Items.Clear(); });
    }

    private async Task LoadItemsAsync()
    {
        Items.Clear();
        IsEmpty = false;
        try
        {
            var items = await _reviewService.GetDueItemsAsync(10);
            foreach (var item in items) Items.Add(item);
            IsEmpty = items.Count == 0;
        }
        catch
        {
            IsEmpty = true;
        }
        CurrentIndex = 0;
        ShowAnswer = false;
    }

    private async Task MarkAsync(bool remembered)
    {
        try
        {
            var item = CurrentItem;
            if (item == null) return;

            if (remembered)
                await _reviewService.MarkRememberedAsync(item);
            else
                await _reviewService.MarkForgottenAsync(item);

            ShowAnswer = false;
            CurrentIndex++;
        }
        catch
        {
            // ignore db errors during review
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
