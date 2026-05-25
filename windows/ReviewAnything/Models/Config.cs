namespace ReviewAnything.Models;

public class Config
{
    public int Id { get; set; }
    public string? ApiBaseUrl { get; set; }
    public string? ApiKey { get; set; }
    public string ModelName { get; set; } = "deepseek-v4-pro";
    public int DailyReviewCount { get; set; } = 10;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
