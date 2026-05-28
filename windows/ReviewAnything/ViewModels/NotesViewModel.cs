using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Microsoft.EntityFrameworkCore;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class NotesViewModel : INotifyPropertyChanged
{
    private AppDbContext Db => _db ??= new AppDbContext();
    private AppDbContext? _db;

    public ObservableCollection<string> Sections { get; } = new();
    public ObservableCollection<Note> Notes { get; } = new();

    private string? _selectedSection;
    public string? SelectedSection
    {
        get => _selectedSection;
        set
        {
            _selectedSection = value;
            OnPropertyChanged();
            if (value != null) _ = LoadNotesAsync(value);
        }
    }

    public ICommand SelectSectionCommand { get; }
    public ICommand DeleteCommand { get; }
    public ICommand CreateSectionCommand { get; }

    public NotesViewModel()
    {
        _ = LoadSectionsAsync();
        SelectSectionCommand = new RelayParamCommand<string>(s => SelectedSection = s);
        DeleteCommand = new AsyncRelayParamCommand<int>(DeleteAsync);
        CreateSectionCommand = new AsyncRelayParamCommand<string>(CreateSectionAsync);
    }

    private async Task LoadSectionsAsync()
    {
        try
        {
            var sections = await Db.Notes
                .Where(n => n.FileName != "_placeholder")
                .Select(n => n.Section)
                .Distinct()
                .ToListAsync();
            var placeholderSections = await Db.Notes
                .Where(n => n.FileName == "_placeholder")
                .Select(n => n.Section)
                .Distinct()
                .ToListAsync();
            var all = sections.Concat(placeholderSections).Distinct().OrderBy(s => s).ToList();

            Sections.Clear();
            foreach (var s in all) Sections.Add(s);
        }
        catch
        {
            // ignore db errors during init
        }
    }

    private async Task LoadNotesAsync(string section)
    {
        var notes = await Db.Notes
            .Where(n => n.Section == section && n.FileName != "_placeholder")
            .ToListAsync();
        Notes.Clear();
        foreach (var n in notes) Notes.Add(n);
    }

    private async Task DeleteAsync(int id)
    {
        var note = await Db.Notes.FindAsync(id);
        if (note != null)
        {
            Db.Notes.Remove(note);
            await Db.SaveChangesAsync();
            if (SelectedSection != null) await LoadNotesAsync(SelectedSection);
            await LoadSectionsAsync();
        }
    }

    private async Task CreateSectionAsync(string name)
    {
        if (string.IsNullOrWhiteSpace(name)) return;
        var trimmed = name.Trim();
        if (Sections.Contains(trimmed)) return;

        try
        {
            // 插入占位 Note 来创建空板块
            Db.Notes.Add(new Note
            {
                FilePath = "",
                FileName = "_placeholder",
                Section = trimmed,
                Content = "",
                ContentHash = ""
            });
            await Db.SaveChangesAsync();
            Sections.Add(trimmed);
            SelectedSection = trimmed;
        }
        catch (Exception ex)
        {
            // 可以在这里添加错误状态属性来显示给用户
            System.Diagnostics.Debug.WriteLine($"Create section failed: {ex.Message}");
        }
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}

public class RelayParamCommand<T> : ICommand
{
    private readonly Action<T> _execute;
    public RelayParamCommand(Action<T> execute) => _execute = execute;
    public bool CanExecute(object? parameter) => true;
    public void Execute(object? parameter) => _execute((T)parameter!);
    public event EventHandler? CanExecuteChanged;
}
