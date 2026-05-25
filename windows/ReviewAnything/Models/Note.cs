namespace ReviewAnything.Models;

public class Note
{
    public int Id { get; set; }
    public string FilePath { get; set; } = "";
    public string FileName { get; set; } = "";
    public string Section { get; set; } = "";
    public string Content { get; set; } = "";
    public string ContentHash { get; set; } = "";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public List<Chunk> Chunks { get; set; } = new();
}
