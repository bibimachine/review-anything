using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;

namespace ReviewAnything.ViewModels;

public class MainViewModel : INotifyPropertyChanged
{
    private object? _currentView;
    public object? CurrentView
    {
        get => _currentView;
        set { _currentView = value; OnPropertyChanged(); }
    }

    public ICommand NavigateReviewCommand { get; }
    public ICommand NavigateUploadCommand { get; }
    public ICommand NavigateNotesCommand { get; }
    public ICommand NavigateCheckInCommand { get; }
    public ICommand NavigateSettingsCommand { get; }

    public MainViewModel()
    {
        NavigateReviewCommand = new RelayCommand(() => CurrentView = new ReviewViewModel());
        NavigateUploadCommand = new RelayCommand(() => CurrentView = new UploadViewModel());
        NavigateNotesCommand = new RelayCommand(() => CurrentView = new NotesViewModel());
        NavigateCheckInCommand = new RelayCommand(() => CurrentView = new CheckInViewModel());
        NavigateSettingsCommand = new RelayCommand(() => CurrentView = new SettingsViewModel());
        CurrentView = new ReviewViewModel();
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}

public class RelayCommand : ICommand
{
    private readonly Action _execute;
    public RelayCommand(Action execute) => _execute = execute;
    public bool CanExecute(object? parameter) => true;
    public void Execute(object? parameter) => _execute();
    public event EventHandler? CanExecuteChanged;
}
