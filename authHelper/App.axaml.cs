using System;
using System.Text.RegularExpressions;
using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;

namespace AnyMCAuthHelper;

public partial class App : Application
{
    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            var args = desktop.Args;
            if (args == null || args.Length < 2)
            {
                Console.Error.WriteLine("Błąd: Two arguments are required: [URL] [Regex]");
                Environment.Exit(1);
            }

            string startUrl = args[0];
            string regexPattern = args[1];

            try
            {
                _ = new Regex(regexPattern);
            }
            catch (ArgumentException)
            {
                Console.Error.WriteLine("Error: The given Regex pattern is invalid.");
                Environment.Exit(1);
            }

            var webDialog = new NativeWebDialog
            {
                Title = "AnyMC Auth Helper",
                CanUserResize = true
            };

            bool isClosing = false;

            webDialog.NavigationStarted += (sender, e) =>
            {
                if (e.Request?.AbsoluteUri == null) return;

                string currentUrl = e.Request.AbsoluteUri;

                if (Regex.IsMatch(currentUrl, regexPattern))
                {
                    e.Cancel = true;

                    if (!isClosing)
                    {
                        isClosing = true;

                        Console.WriteLine(currentUrl);

                        webDialog.Close();
                    }
                }
            };

            webDialog.Show();
            webDialog.Source = new Uri(startUrl);

            webDialog.Closing += (sender, e) =>
            {
                desktop.Shutdown();
            };
        }

        base.OnFrameworkInitializationCompleted();
    }
}