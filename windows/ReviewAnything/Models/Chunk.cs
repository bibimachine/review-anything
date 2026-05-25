namespace ReviewAnything.Models;

public class Chunk
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;
    public string Content { get; set; } = "";
    public string ContentHash { get; set; } = "";
    public string HeadingPath { get; set; } = "";
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public List<ReviewItem> ReviewItems { get; set; } = new();
}
