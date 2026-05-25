namespace ReviewAnything.Models;

public class CheckIn
{
    public int Id { get; set; }
    public string CheckinDate { get; set; } = ""; // YYYY-MM-DD
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
