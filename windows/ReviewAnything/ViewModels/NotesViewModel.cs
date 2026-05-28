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
            if (value != null) LoadNotes(value);
        }
    }

    public ICommand SelectSectionCommand { get; }
    public ICommand DeleteCommand { get; }
    public ICommand CreateSectionCommand { get; }

    public NotesViewModel()
    {
        LoadSections();
        SelectSectionCommand = new RelayParamCommand<string>(s => SelectedSection = s);
        DeleteCommand = new RelayParamCommand<int>(async id => await DeleteAsync(id));
        CreateSectionCommand = new RelayParamCommand<string>(async name => await CreateSectionAsync(name));
    }

    private async void LoadSections()
    {
        try
        {
            var sections = await Db.Notes
                .Where(n => n.FileName != "_placeholder")
                .Select(n => n.Section)
                .Distinct()
                .ToListAsync();
            // 也包含占位符板块的 section
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

    private async void LoadNotes(string section)
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
            if (SelectedSection != null) LoadNotes(SelectedSection);
            LoadSections();
        }
    }

    private async Task CreateSectionAsync(string name)
    {
        if (string.IsNullOrWhiteSpace(name)) return;
        var trimmed = name.Trim();
        if (Sections.Contains(trimmed)) return;

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
