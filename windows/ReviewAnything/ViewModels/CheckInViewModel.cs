using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class CheckInViewModel : INotifyPropertyChanged
{
    private AppDbContext Db => _db ??= new AppDbContext();
    private AppDbContext? _db;

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

    private List<string> _checkInDates = new();
    public List<string> CheckInDates
    {
        get => _checkInDates;
        set { _checkInDates = value; OnPropertyChanged(); }
    }

    public ICommand CheckInCommand { get; }

    public CheckInViewModel()
    {
        LoadCheckIns();
        CheckInCommand = new RelayCommand(async () => await CheckInAsync());
    }

    private void LoadCheckIns()
    {
        var dates = Db.CheckIns.Select(c => c.CheckinDate).ToList();
        CheckInDates = dates;
        CheckedToday = dates.Contains(DateTime.Now.ToString("yyyy-MM-dd"));
        Streak = CalculateStreak(dates.ToHashSet());
    }

    private async Task CheckInAsync()
    {
        var today = DateTime.Now.ToString("yyyy-MM-dd");
        if (!Db.CheckIns.Any(c => c.CheckinDate == today))
        {
            Db.CheckIns.Add(new CheckIn { CheckinDate = today });
            await Db.SaveChangesAsync();
            LoadCheckIns();
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
