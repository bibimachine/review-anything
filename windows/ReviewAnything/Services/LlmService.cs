using Newtonsoft.Json.Linq;
using ReviewAnything.Models;
using System.Net.Http.Headers;
using System.Text;

namespace ReviewAnything.Services;

public class LlmService
{
    private readonly HttpClient _client = new()
    {
        Timeout = TimeSpan.FromSeconds(120)
    };

    public async Task<List<(string Question, string Answer)>> GenerateQAAsync(string content, string headingPath, Config config)
    {
        var systemPrompt = """你是一个帮助用户复习笔记的助手。请根据提供的笔记内容生成3个不同的复习问题及其答案。
要求：1. 每个问题考察不同的知识点或角度 2. 问题应该考察理解，而不是简单记忆 3. 答案简洁准确，覆盖核心要点
4. 请用JSON格式返回数组：[{"question": "问题1", "answer": "答案1"}]""";

        var context = $"笔记标题路径: {headingPath}\n\n笔记内容:\n{content}";
        var baseUrl = (config.ApiBaseUrl ?? "https://api.deepseek.com").TrimEnd('/');

        var body = new JObject
        {
            ["model"] = config.ModelName,
            ["messages"] = new JArray
            {
                new JObject { ["role"] = "system", ["content"] = systemPrompt },
                new JObject { ["role"] = "user", ["content"] = context }
            },
            ["temperature"] = 0.8,
            ["reasoning_effort"] = "high",
            ["thinking"] = new JObject { ["type"] = "enabled" }
        };

        var request = new HttpRequestMessage(HttpMethod.Post, $"{baseUrl}/chat/completions")
        {
            Content = new StringContent(body.ToString(), Encoding.UTF8, "application/json")
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", config.ApiKey);

        var response = await _client.SendAsync(request);
        response.EnsureSuccessStatusCode();

        var json = JObject.Parse(await response.Content.ReadAsStringAsync());
        var result = json["choices"]?[0]?["message"]?["content"]?.ToString() ?? "";
        return ParseQA(result);
    }

    private static List<(string, string)> ParseQA(string content)
    {
        try
        {
            var array = JArray.Parse(content);
            return array.Select(t => (t["question"]?.ToString() ?? "", t["answer"]?.ToString() ?? "")).ToList();
        }
        catch
        {
            var start = content.IndexOf('[');
            var end = content.LastIndexOf(']');
            if (start != -1 && end != -1)
            {
                var array = JArray.Parse(content.Substring(start, end - start + 1));
                return array.Select(t => (t["question"]?.ToString() ?? "", t["answer"]?.ToString() ?? "")).ToList();
            }
            return new List<(string, string)>();
        }
    }
}
