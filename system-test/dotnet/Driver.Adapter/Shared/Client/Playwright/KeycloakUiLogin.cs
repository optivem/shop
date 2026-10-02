using Driver.Adapter.Shared.Client.Http;
using Microsoft.Playwright;

namespace Driver.Adapter.Shared.Client.Playwright;

/// <summary>Fills in the Keycloak login form when the browser has been redirected to it.</summary>
public sealed class KeycloakUiLogin
{
    private const string UsernameSelector = "#username";
    private const string PasswordSelector = "#password";
    private const string SubmitSelector = "#kc-login";
    private const int TimeoutMilliseconds = 90_000;

    private readonly string _keycloakBaseUrl;
    private readonly TestUser _user;

    private KeycloakUiLogin(string keycloakBaseUrl, TestUser user)
    {
        _keycloakBaseUrl = keycloakBaseUrl;
        _user = user;
    }

    /// <summary>Returns null when no Keycloak URL is configured (the SUT then has no login).</summary>
    public static KeycloakUiLogin? ForBaseUrl(string? keycloakBaseUrl)
    {
        if (string.IsNullOrWhiteSpace(keycloakBaseUrl))
            return null;
        return new KeycloakUiLogin(keycloakBaseUrl, TestUser.Admin);
    }

    public bool IsLoginPage(IPage page) => page.Url.StartsWith(_keycloakBaseUrl, StringComparison.Ordinal);

    /// <summary>Logs in if the page is on the Keycloak login form; returns true when a login was performed.</summary>
    public async Task<bool> LoginIfRequiredAsync(IPage page)
    {
        if (!IsLoginPage(page))
            return false;
        await LoginAsync(page);
        return true;
    }

    /// <summary>Waits until either the Keycloak login form or the app-ready selector shows, logging in if needed.</summary>
    public async Task EnsureLoggedInAsync(IPage page, string appReadySelector)
    {
        await page.WaitForSelectorAsync(SubmitSelector + ", " + appReadySelector,
            new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        if (IsLoginPage(page))
        {
            await LoginAsync(page);
            await page.WaitForSelectorAsync(appReadySelector,
                new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        }
    }

    private async Task LoginAsync(IPage page)
    {
        await page.WaitForSelectorAsync(SubmitSelector, new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        await page.FillAsync(UsernameSelector, _user.Username());
        await page.FillAsync(PasswordSelector, _user.Password());
        await page.ClickAsync(SubmitSelector);
        await page.WaitForURLAsync(url => !url.StartsWith(_keycloakBaseUrl, StringComparison.Ordinal),
            new PageWaitForURLOptions { Timeout = TimeoutMilliseconds, WaitUntil = WaitUntilState.Commit });
    }
}
