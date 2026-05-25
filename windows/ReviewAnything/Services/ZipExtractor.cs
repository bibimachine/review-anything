using System.IO.Compression;

namespace ReviewAnything.Services;

public static class ZipExtractor
{
    public static List<(string FileName, string Content)> Extract(string zipPath)
    {
        var results = new List<(string, string)>();
        var tempDir = Path.Combine(Path.GetTempPath(), $"ra_extract_{Guid.NewGuid()}");
        Directory.CreateDirectory(tempDir);

        try
        {
            ZipFile.ExtractToDirectory(zipPath, tempDir);
            foreach (var file in Directory.GetFiles(tempDir, "*.md", SearchOption.AllDirectories))
            {
                var relPath = Path.GetRelativePath(tempDir, file);
                var content = File.ReadAllText(file);
                results.Add((relPath, content));
            }
        }
        finally
        {
            Directory.Delete(tempDir, true);
        }

        return results;
    }
}
