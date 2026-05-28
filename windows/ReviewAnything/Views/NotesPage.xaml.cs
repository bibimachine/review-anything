using System.Windows;
using System.Windows.Controls;

namespace ReviewAnything.Views;

public partial class NotesPage : Page
{
    public NotesPage()
    {
        InitializeComponent();
    }

    private void NewSectionButton_Click(object sender, RoutedEventArgs e)
    {
        NewSectionText.Text = "";
    }
}
