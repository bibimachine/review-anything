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
    private readonly AppDbContext _db = new();

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

    public NotesViewModel()
    {
        LoadSections();
        SelectSectionCommand = new RelayParamCommand<string>(s => SelectedSection = s);
        DeleteCommand = new RelayParamCommand<int>(async id => await DeleteAsync(id));
    }

    private async void LoadSections()
    {
        try
        {
            var sections = await _db.Notes.Select(n => n.Section).Distinct().ToListAsync();
            Sections.Clear();
            foreach (var s in sections) Sections.Add(s);
        }
        catch
        {
            // ignore db errors during init
        }
    }

    private async void LoadNotes(string section)
    {
        var notes = await _db.Notes.Where(n => n.Section == section).ToListAsync();
        Notes.Clear();
        foreach (var n in notes) Notes.Add(n);
    }

    private async Task DeleteAsync(int id)
    {
        var note = await _db.Notes.FindAsync(id);
        if (note != null)
        {
            _db.Notes.Remove(note);
            await _db.SaveChangesAsync();
            if (SelectedSection != null) LoadNotes(SelectedSection);
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
