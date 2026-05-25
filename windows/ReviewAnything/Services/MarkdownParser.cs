using System.Security.Cryptography;
using System.Text;

namespace ReviewAnything.Services;

public record ParsedChunk(string Content, string HeadingPath);

public static class MarkdownParser
{
    public static List<ParsedChunk> Parse(string content)
    {
        var lines = content.Split('\n');
        var chunks = new List<ParsedChunk>();
        var headingStack = new List<string>();
        var currentContent = new StringBuilder();

        void FlushChunk()
        {
            var text = currentContent.ToString().Trim();
            if (text.Length >= 20)
            {
                chunks.Add(new ParsedChunk(text, string.Join("/", headingStack)));
            }
            currentContent.Clear();
        }

        foreach (var line in lines)
        {
            var trimmed = line.Trim();
            if (trimmed.StartsWith("#"))
            {
                FlushChunk();
                var level = trimmed.TakeWhile(c => c == '#').Count();
                var title = trimmed[level..].Trim();
                while (headingStack.Count >= level)
                {
                    headingStack.RemoveAt(headingStack.Count - 1);
                }
                headingStack.Add(title);
            }
            else
            {
                currentContent.AppendLine(line);
            }
        }
        FlushChunk();

        return chunks.Count > 0 ? chunks : new List<ParsedChunk> { new(content.Trim(), "") };
    }

    public static string ComputeHash(string content)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(content));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }
}
