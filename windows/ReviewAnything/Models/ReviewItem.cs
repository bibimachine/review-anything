namespace ReviewAnything.Models;

public class ReviewItem
{
    public int Id { get; set; }
    public int ChunkId { get; set; }
    public Chunk Chunk { get; set; } = null!;
    public string Question { get; set; } = "";
    public string Answer { get; set; } = "";
    public bool IsHard { get; set; }
    public int ReviewCount { get; set; }
    public DateTime NextReviewAt { get; set; } = DateTime.UtcNow;
    public DateTime? LastReviewedAt { get; set; }
    public bool LlmFailed { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
