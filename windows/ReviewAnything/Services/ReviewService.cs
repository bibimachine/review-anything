using Microsoft.EntityFrameworkCore;
using ReviewAnything.Models;

namespace ReviewAnything.Services;

public class ReviewService
{
    private readonly AppDbContext _db;
    public ReviewService(AppDbContext db) => _db = db;

    public async Task<List<ReviewItem>> GetDueItemsAsync(int count)
    {
        return await _db.ReviewItems
            .Where(r => r.NextReviewAt <= DateTime.UtcNow)
            .OrderBy(r => r.NextReviewAt)
            .Take(count)
            .ToListAsync();
    }

    public async Task MarkRememberedAsync(ReviewItem item)
    {
        var newCount = item.ReviewCount + 1;
        var intervalDays = newCount switch
        {
            1 => 1,
            2 => 3,
            3 => 7,
            4 => 14,
            _ => 30
        };
        item.ReviewCount = newCount;
        item.NextReviewAt = DateTime.UtcNow.AddDays(intervalDays);
        item.LastReviewedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }

    public async Task MarkForgottenAsync(ReviewItem item)
    {
        item.IsHard = true;
        item.NextReviewAt = DateTime.UtcNow.AddDays(1);
        item.LastReviewedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();
    }
}
