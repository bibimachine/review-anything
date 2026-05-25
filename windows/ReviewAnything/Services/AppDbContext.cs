using Microsoft.EntityFrameworkCore;
using ReviewAnything.Models;

namespace ReviewAnything.Services;

public class AppDbContext : DbContext
{
    public DbSet<Config> Configs { get; set; }
    public DbSet<Note> Notes { get; set; }
    public DbSet<Chunk> Chunks { get; set; }
    public DbSet<ReviewItem> ReviewItems { get; set; }
    public DbSet<CheckIn> CheckIns { get; set; }

    protected override void OnConfiguring(DbContextOptionsBuilder options)
    {
        var dbPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "ReviewAnything", "review_anything.db");
        Directory.CreateDirectory(Path.GetDirectoryName(dbPath)!);
        options.UseSqlite($"Data Source={dbPath}");
    }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Note>()
            .HasMany(n => n.Chunks)
            .WithOne(c => c.Note)
            .HasForeignKey(c => c.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Chunk>()
            .HasMany(c => c.ReviewItems)
            .WithOne(r => r.Chunk)
            .HasForeignKey(r => r.ChunkId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<ReviewItem>()
            .HasIndex(r => r.NextReviewAt);
    }
}
