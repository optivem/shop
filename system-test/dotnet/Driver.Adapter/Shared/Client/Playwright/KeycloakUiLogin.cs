using Driver.Adapter.Shared.Client.Http;
using Microsoft.Playwright;

namespace Driver.Adapter.Shared.Client.Playwright;

/// <summary>Fills in the Keycloak login form when the browser has been redirected to it.</summary>
public sealed class KeycloakUiLogin
{
    private const string UsernameSelector = "#username";
    private const string PasswordSelector = "#password";
    private const string SubmitSelector = "#kc-login";
    private const string RegisterFormSelector = "#kc-register-form";
    private const string RegisterSubmitSelector = "#kc-register-form [type='submit']";
    /// <summary>The field errors Keycloak shows on a rejected registration (e.g. a username that is taken).</summary>
    private const string RegisterErrorSelector = "#kc-register-form .kc-feedback-text";
    private const string PasswordConfirmSelector = "#password-confirm";
    private const string EmailSelector = "#email";
    private const string FirstNameSelector = "#firstName";
    private const string LastNameSelector = "#lastName";
    private const string CountrySelector = "#country";
    /// <summary>The storefront's header link for anonymous visitors; apps that force login on every page never show it.</summary>
    private const string SignInSelector = "[data-testid='sign-in']";
    private const int TimeoutMilliseconds = 90_000;

    private readonly string _keycloakBaseUrl;
    private readonly TestUser _user;

    private KeycloakUiLogin(string keycloakBaseUrl, TestUser user)
    {
        _keycloakBaseUrl = keycloakBaseUrl;
        _user = user;
    }

    /// <summary>Returns null when no Keycloak URL is configured (the SUT then has no login).</summary>
    public static KeycloakUiLogin? ForBaseUrl(string? keycloakBaseUrl, TestUser? user = null)
    {
        if (string.IsNullOrWhiteSpace(keycloakBaseUrl))
            return null;
        return new KeycloakUiLogin(keycloakBaseUrl, user ?? TestUser.Customer);
    }

    public TestUser User => _user;

    public bool IsLoginPage(IPage page) => page.Url.StartsWith(_keycloakBaseUrl, StringComparison.Ordinal);

    /// <summary>Logs in if the page is on the Keycloak login form; returns true when a login was performed.</summary>
    public async Task<bool> LoginIfRequiredAsync(IPage page)
    {
        if (!IsLoginPage(page))
            return false;
        await LoginAsync(page);
        return true;
    }

    /// <summary>
    /// Waits until the Keycloak login form, the app-ready selector or the "Sign In" link shows, and logs in if needed.
    /// An app with an anonymous storefront shows "Sign In" instead of redirecting, so it is clicked to reach the login.
    /// </summary>
    public async Task EnsureLoggedInAsync(IPage page, string appReadySelector)
    {
        await page.WaitForSelectorAsync(SubmitSelector + ", " + appReadySelector + ", " + SignInSelector,
            new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        if (!IsLoginPage(page) && !await page.IsVisibleAsync(appReadySelector) && await page.IsVisibleAsync(SignInSelector))
        {
            await page.ClickAsync(SignInSelector);
            // Keycloak shows its form, or sends the browser straight back when it already has a session.
            await page.WaitForSelectorAsync(SubmitSelector + ", " + appReadySelector,
                new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        }
        if (IsLoginPage(page))
        {
            await LoginAsync(page);
            await page.WaitForSelectorAsync(appReadySelector,
                new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        }
    }

    /// <summary>
    /// Fills in and submits Keycloak's registration form (UC-013) for this login's user, then waits until the app
    /// shows <paramref name="appReadySelector"/>.
    /// </summary>
    /// <returns>null when the app was reached, else the errors the registration form showed</returns>
    public async Task<string?> RegisterAsync(IPage page, string email, string firstName, string lastName, string country,
        string appReadySelector)
    {
        await page.WaitForSelectorAsync(RegisterFormSelector, new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        await page.FillAsync(UsernameSelector, _user.Username);
        await page.FillAsync(PasswordSelector, _user.Password);
        await page.FillAsync(PasswordConfirmSelector, _user.Password);
        await page.FillAsync(EmailSelector, email);
        await page.FillAsync(FirstNameSelector, firstName);
        await page.FillAsync(LastNameSelector, lastName);
        await page.SelectOptionAsync(CountrySelector, country);
        await page.ClickAsync(RegisterSubmitSelector);
        await page.WaitForSelectorAsync(appReadySelector + ", " + RegisterErrorSelector,
            new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        if (IsLoginPage(page))
        {
            return string.Join("; ", await page.Locator(RegisterErrorSelector).AllInnerTextsAsync());
        }
        return null;
    }

    /// <summary>Waits for the login form, without filling it in.</summary>
    public async Task WaitForLoginFormAsync(IPage page)
    {
        await page.WaitForSelectorAsync(SubmitSelector, new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
    }

    private async Task LoginAsync(IPage page)
    {
        await page.WaitForSelectorAsync(SubmitSelector, new PageWaitForSelectorOptions { Timeout = TimeoutMilliseconds });
        await page.FillAsync(UsernameSelector, _user.Username);
        await page.FillAsync(PasswordSelector, _user.Password);
        await page.ClickAsync(SubmitSelector);
        // Poll location instead of WaitForURLAsync, which also waits on a load-state event that can hang on the redirect back.
        await page.WaitForFunctionAsync("keycloakBase => !location.href.startsWith(keycloakBase)", _keycloakBaseUrl,
            new PageWaitForFunctionOptions { Timeout = TimeoutMilliseconds });
    }
}
