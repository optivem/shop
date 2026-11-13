using System.Collections.Concurrent;

using Microsoft.Playwright;

namespace Driver.Adapter.Ui.Client;

/// <summary>
/// Shares Chromium browsers across UI tests, the .NET counterpart of Java's BrowserLifecycleExtension
/// (one browser per worker thread). xUnit async tests do not stay on one thread, so instead of a browser
/// per thread each test rents an idle browser and returns it when it is done; every test still gets its
/// own isolated browser context. One Playwright driver serves all browsers in the process.
/// Launching a Playwright driver and Chromium for every test made parallel UI suites stall all at once
/// on loaded CI runners (REL #68).
/// </summary>
internal static class BrowserPool
{
    private static readonly SemaphoreSlim PlaywrightLock = new(1, 1);
    private static readonly ConcurrentBag<IBrowser> Idle = new();
    private static readonly ConcurrentBag<IBrowser> All = new();
    private static IPlaywright? _playwright;

    static BrowserPool()
    {
        AppDomain.CurrentDomain.ProcessExit += (_, _) => CloseAll();
    }

    public static async Task<IBrowser> RentAsync(bool headless)
    {
        while (Idle.TryTake(out var browser))
        {
            if (browser.IsConnected)
            {
                return browser;
            }
        }

        var playwright = await GetPlaywrightAsync();
        var launched = await playwright.Chromium.LaunchAsync(new BrowserTypeLaunchOptions { Headless = headless });
        All.Add(launched);
        return launched;
    }

    /// <summary>Returns a browser for the next test. A disconnected browser is dropped.</summary>
    public static void Return(IBrowser browser)
    {
        if (browser.IsConnected)
        {
            Idle.Add(browser);
        }
    }

    private static async Task<IPlaywright> GetPlaywrightAsync()
    {
        if (_playwright != null)
        {
            return _playwright;
        }

        await PlaywrightLock.WaitAsync();
        try
        {
            return _playwright ??= await Playwright.CreateAsync();
        }
        finally
        {
            PlaywrightLock.Release();
        }
    }

    private static void CloseAll()
    {
        foreach (var browser in All)
        {
            try
            {
                browser.CloseAsync().GetAwaiter().GetResult();
            }
            catch (Exception)
            {
                // Best effort at process exit: the driver process goes away with this one anyway.
            }
        }

        _playwright?.Dispose();
    }
}
