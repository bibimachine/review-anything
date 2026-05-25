using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Microsoft.Win32;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class UploadViewModel : INotifyPropertyChanged
{
    private readonly AppDbContext _db = new();
    private readonly LlmService _llmService = new();

    private string _status = "选择 ZIP 文件上传";
    public string Status
    {
        get => _status;
        set { _status = value; OnPropertyChanged(); }
    }

    private int _current;
    public int Current
    {
        get => _current;
        set { _current = value; OnPropertyChanged(); OnPropertyChanged(nameof(ProgressPercent)); }
    }

    private int _total;
    public int Total
    {
        get => _total;
        set { _total = value; OnPropertyChanged(); OnPropertyChanged(nameof(ProgressPercent)); }
    }

    public double ProgressPercent => Total > 0 ? (double)Current / Total * 100 : 0;

    public ICommand UploadCommand { get; }

    public UploadViewModel()
    {
        UploadCommand = new RelayCommand(async () => await UploadAsync());
    }

    private async Task UploadAsync()
    {
        var dialog = new OpenFileDialog
        {
            Filter = "ZIP files (*.zip)|*.zip",
            Title = "选择笔记 ZIP 文件"
        };

        if (dialog.ShowDialog() != true) return;

        Status = "解压中...";
        var files = ZipExtractor.Extract(dialog.FileName);

        var allChunks = files.SelectMany(f => MarkdownParser.Parse(f.Content)).ToList();
        Total = allChunks.Count;
        Current = 0;

        foreach (var (fileName, content) in files)
        {
            var section = fileName.Split('/', '\\').FirstOrDefault() ?? "未分类";
            var note = new Note
            {
                FilePath = fileName,
                FileName = Path.GetFileName(fileName),
                Section = section,
                Content = content,
                ContentHash = MarkdownParser.ComputeHash(content)
            };
            _db.Notes.Add(note);
            await _db.SaveChangesAsync();

            var chunks = MarkdownParser.Parse(content);
            foreach (var chunkData in chunks)
            {
                Current++;
                Status = $"处理中... {Current}/{Total}";

                var chunk = new Chunk
                {
                    NoteId = note.Id,
                    Content = chunkData.Content,
                    ContentHash = MarkdownParser.ComputeHash(chunkData.Content),
                    HeadingPath = chunkData.HeadingPath
                };
                _db.Chunks.Add(chunk);
                await _db.SaveChangesAsync();

                // LLM 生成 QA
                var config = _db.Configs.FirstOrDefault();
                if (config?.ApiKey != null)
                {
                    try
                    {
                        var qaList = await _llmService.GenerateQAAsync(
                            chunkData.Content, chunkData.HeadingPath, config);
                        foreach (var (q, a) in qaList)
                        {
                            _db.ReviewItems.Add(new ReviewItem
                            {
                                ChunkId = chunk.Id,
                                Question = q,
                                Answer = a
                            });
                        }
                    }
                    catch
                    {
                        _db.ReviewItems.Add(new ReviewItem
                        {
                            ChunkId = chunk.Id,
                            Question = $"请解释「{chunkData.HeadingPath}" + "」的核心要点？",
                            Answer = chunkData.Content[..Math.Min(300, chunkData.Content.Length)],
                            LlmFailed = true
                        });
                    }
                }
                else
                {
                    _db.ReviewItems.Add(new ReviewItem
                    {
                        ChunkId = chunk.Id,
                        Question = $"请解释「{chunkData.HeadingPath}" + "」的核心要点？",
                        Answer = chunkData.Content[..Math.Min(300, chunkData.Content.Length)],
                        LlmFailed = true
                    });
                }
                await _db.SaveChangesAsync();
            }
        }

        Status = $"上传完成！{files.Count} 个文件，{Total} 个段落";
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
