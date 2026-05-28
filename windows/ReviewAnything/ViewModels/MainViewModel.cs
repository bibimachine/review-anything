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
    public ICommand NavigateSettingsCommand { get; }

    public MainViewModel()
    {
        NavigateReviewCommand = new RelayCommand(() => CurrentView = new ReviewViewModel());
        NavigateUploadCommand = new RelayCommand(() => CurrentView = new UploadViewModel());
        NavigateNotesCommand = new RelayCommand(() => CurrentView = new NotesViewModel());
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

public class AsyncRelayCommand : ICommand
{
    private readonly Func<Task> _execute;
    private bool _isExecuting;
    public AsyncRelayCommand(Func<Task> execute) => _execute = execute;
    public bool CanExecute(object? parameter) => !_isExecuting;
    public async void Execute(object? parameter)
    {
        _isExecuting = true;
        CanExecuteChanged?.Invoke(this, EventArgs.Empty);
        try { await _execute(); }
        catch { /* exception handled inside */ }
        finally
        {
            _isExecuting = false;
            CanExecuteChanged?.Invoke(this, EventArgs.Empty);
        }
    }
    public event EventHandler? CanExecuteChanged;
}

public class AsyncRelayParamCommand<T> : ICommand
{
    private readonly Func<T, Task> _execute;
    private bool _isExecuting;
    public AsyncRelayParamCommand(Func<T, Task> execute) => _execute = execute;
    public bool CanExecute(object? parameter) => !_isExecuting;
    public async void Execute(object? parameter)
    {
        _isExecuting = true;
        CanExecuteChanged?.Invoke(this, EventArgs.Empty);
        try { await _execute((T)parameter!); }
        catch { /* exception handled inside */ }
        finally
        {
            _isExecuting = false;
            CanExecuteChanged?.Invoke(this, EventArgs.Empty);
        }
    }
    public event EventHandler? CanExecuteChanged;
}
