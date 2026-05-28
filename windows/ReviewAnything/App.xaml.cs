using System.Windows;
using ReviewAnything.Services;

namespace ReviewAnything;

public partial class App : Application
{
    protected override void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);
        try
        {
            using var db = new AppDbContext();
            db.Database.EnsureCreated();
        }
        catch (Exception ex)
        {
            MessageBox.Show($"数据库初始化失败: {ex.Message}\n\n请检查磁盘空间和权限。", "启动错误", MessageBoxButton.OK, MessageBoxImage.Error);
            Shutdown();
        }
    }
}
