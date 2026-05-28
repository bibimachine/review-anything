using System.ComponentModel;
using System.IO;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using Microsoft.EntityFrameworkCore;
using Microsoft.Win32;
using ReviewAnything.Models;
using ReviewAnything.Services;

namespace ReviewAnything.ViewModels;

public class UploadViewModel : INotifyPropertyChanged
{
    private AppDbContext Db => _db ??= new AppDbContext();
    private AppDbContext? _db;
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

    private string _section = "";
    public string Section
    {
        get => _section;
        set { _section = value; OnPropertyChanged(); }
    }

    public double ProgressPercent => Total > 0 ? (double)Current / Total * 100 : 0;

    public ICommand UploadCommand { get; }

    public UploadViewModel()
    {
        UploadCommand = new AsyncRelayCommand(UploadAsync);
    }

    private async Task UploadAsync()
    {
        try
        {
            var dialog = new OpenFileDialog
            {
                Filter = "ZIP files (*.zip)|*.zip",
                Title = "选择笔记 ZIP 文件"
            };

            if (dialog.ShowDialog() != true) return;

            Status = "解压中...";
            var files = await Task.Run(() => ZipExtractor.Extract(dialog.FileName));
            if (files.Count == 0)
            {
                Status = "ZIP 中没有找到 Markdown 文件";
                return;
            }

            var allChunks = await Task.Run(() => files.SelectMany(f => MarkdownParser.Parse(f.Content)).ToList());
            Total = allChunks.Count;
            Current = 0;

            foreach (var (fileName, content) in files)
            {
                var section = string.IsNullOrWhiteSpace(Section)
                    ? (fileName.Split('/', '\\').FirstOrDefault() ?? "未分类")
                    : Section.Trim();
                var note = new Note
                {
                    FilePath = fileName,
                    FileName = Path.GetFileName(fileName),
                    Section = section,
                    Content = content,
                    ContentHash = MarkdownParser.ComputeHash(content)
                };
                Db.Notes.Add(note);
                await Db.SaveChangesAsync();

                var chunks = await Task.Run(() => MarkdownParser.Parse(content));
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
                    Db.Chunks.Add(chunk);
                    await Db.SaveChangesAsync();

                    // LLM 生成 QA
                    var config = await Db.Configs.FirstOrDefaultAsync();
                    if (config?.ApiKey != null)
                    {
                        try
                        {
                            var qaList = await _llmService.GenerateQAAsync(
                                chunkData.Content, chunkData.HeadingPath, config);
                            foreach (var (q, a) in qaList)
                            {
                                Db.ReviewItems.Add(new ReviewItem
                                {
                                    ChunkId = chunk.Id,
                                    Question = q,
                                    Answer = a
                                });
                            }
                        }
                        catch
                        {
                            AddFallbackReviewItem(chunk.Id, chunkData);
                        }
                    }
                    else
                    {
                        AddFallbackReviewItem(chunk.Id, chunkData);
                    }
                    await Db.SaveChangesAsync();
                }
            }

            Status = $"上传完成！{files.Count} 个文件，{Total} 个段落";
        }
        catch (Exception ex)
        {
            Status = $"上传失败: {ex.Message}";
        }
    }

    private void AddFallbackReviewItem(int chunkId, ParsedChunk chunkData)
    {
        var question = string.IsNullOrWhiteSpace(chunkData.HeadingPath)
            ? "请解释这段内容的核心要点？"
            : $"请解释「{chunkData.HeadingPath}」的核心要点？";
        Db.ReviewItems.Add(new ReviewItem
        {
            ChunkId = chunkId,
            Question = question,
            Answer = chunkData.Content[..Math.Min(300, chunkData.Content.Length)],
            LlmFailed = true
        });
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    protected void OnPropertyChanged([CallerMemberName] string name = "") =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
