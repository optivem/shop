using Microsoft.Playwright;

namespace Driver.Adapter.Shared.Client.Playwright;

public class PageClient
{
    private readonly IPage _page;
    private readonly float _timeoutMilliseconds;
    private readonly KeycloakUiLogin? _login;

    // 30 second timeout for better stability
    private const int DefaultTimeoutSeconds = 30;
    private const int DefaultTimeoutMilliseconds = DefaultTimeoutSeconds * 1000;

    private PageClient(IPage page, float timeoutMilliseconds, KeycloakUiLogin? login)
    {
        _page = page;
        _timeoutMilliseconds = timeoutMilliseconds;
        _login = login;
    }

    public PageClient(IPage page, string baseUrl, KeycloakUiLogin? login = null)
        : this(page, DefaultTimeoutMilliseconds, login)
    {
    }

    // When the app redirected the browser to the Keycloak login form, sign in before touching the page.
    private async Task LoginIfRedirectedAsync()
    {
        if (_login != null)
        {
            await _login.LoginIfRequiredAsync(_page);
        }
    }

    public ILocator GetLocator(string selector)
    {
        return _page.Locator(selector);
    }

    public async Task FillAsync(string selector, string? text)
    {
        var locator = await GetLocatorAsync(selector);
        var processedValue = text ?? string.Empty;
        await locator.FillAsync(processedValue);
    }

    public async Task ClickAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        await locator.ClickAsync();
    }

    public async Task<string> ReadTextContentAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        return await locator.TextContentAsync() ?? string.Empty;
    }

    public async Task<string> ReadInputValueAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        return await locator.InputValueAsync();
    }

    public async Task<string> ReadTextContentImmediatelyAsync(string selector)
    {
        var locator = _page.Locator(selector);
        return await locator.TextContentAsync() ?? string.Empty;
    }

    public async Task<List<string>> ReadAllTextContentsAsync(string selector)
    {
        var locator = _page.Locator(selector);
        await LoginIfRedirectedAsync();
        // Wait for at least one element to be visible
        // AllTextContentsAsync() doesn't trigger strict mode - it's designed for multiple elements
        await locator.First.WaitForAsync(GetDefaultWaitForOptions());
        var contents = await locator.AllTextContentsAsync();
        return contents.ToList();
    }

    public async Task<List<string>> ReadAllTextContentsNoWaitAsync(string selector)
    {
        var contents = await _page.Locator(selector).AllTextContentsAsync();
        return contents.ToList();
    }

    public async Task WaitForVisibleAsync(string selector, float? timeoutMilliseconds = null)
    {
        await LoginIfRedirectedAsync();
        var locator = _page.Locator(selector);
        var options = timeoutMilliseconds.HasValue
            ? new LocatorWaitForOptions { State = WaitForSelectorState.Visible, Timeout = timeoutMilliseconds.Value }
            : GetDefaultWaitForOptions();
        await locator.WaitForAsync(options);
    }

    /// <summary>Waits until an element matches the selector, shown or not (such as an option of a select).</summary>
    public async Task WaitForAttachedAsync(string selector)
    {
        await _page.Locator(selector).First.WaitForAsync(new LocatorWaitForOptions
        {
            State = WaitForSelectorState.Attached,
            Timeout = _timeoutMilliseconds,
        });
    }

    public async Task<bool> IsVisibleAsync(string selector)
    {
        try
        {
            var locator = _page.Locator(selector);
            await locator.First.WaitForAsync(GetDefaultWaitForOptions());
            return await locator.CountAsync() > 0;
        }
        catch (Exception)
        {
            return false;
        }
    }

    public async Task PressAsync(string selector, string key)
    {
        var locator = await GetLocatorAsync(selector);
        await locator.PressAsync(key);
    }

    /// <summary>Whether the element belongs to a form, so acting on it posts the form and loads a new document.</summary>
    public async Task<bool> BelongsToFormAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        return await locator.EvaluateAsync<bool>("element => element.form != null");
    }

    /// <summary>Runs an action that loads a new document (such as a form post), and waits until the new document is ready.</summary>
    public async Task RunAndWaitForNewDocumentAsync(Func<Task> action)
    {
        await _page.EvaluateAsync("() => { window.__previousDocument = true; }");
        await action();
        await _page.WaitForFunctionAsync("() => window.__previousDocument === undefined && document.readyState !== 'loading'", null,
            new PageWaitForFunctionOptions { Timeout = _timeoutMilliseconds });
    }

    /// <summary>Moves a single-page app to a route without reloading it.</summary>
    public async Task NavigateWithinAppAsync(string route)
    {
        await _page.EvaluateAsync("route => { history.pushState({}, '', route); dispatchEvent(new PopStateEvent('popstate')); }", route);
    }

    public async Task<string?> ReadAttributeAsync(string selector, string attributeName)
    {
        var locator = _page.Locator(selector);
        return await locator.GetAttributeAsync(attributeName);
    }

    /// <summary>Reads an attribute of every element the selector matches, without waiting for any.</summary>
    public async Task<List<string?>> ReadAllAttributesNoWaitAsync(string selector, string attributeName)
    {
        var values = new List<string?>();
        foreach (var locator in await _page.Locator(selector).AllAsync())
        {
            values.Add(await locator.GetAttributeAsync(attributeName));
        }
        return values;
    }

    public async Task SelectOptionByLabelAsync(string selector, string label)
    {
        var locator = await GetLocatorAsync(selector);
        await locator.SelectOptionAsync(new SelectOptionValue { Label = label });
    }

    /// <summary>Selects the option with this value; a value the select does not offer is added first, as an edited form would send it.</summary>
    public async Task SelectOptionByValueAddingIfMissingAsync(string selector, string value)
    {
        var locator = await GetLocatorAsync(selector);
        await locator.EvaluateAsync("(select, value) => { if (![...select.options].some(o => o.value === value)) {"
            + " const option = document.createElement('option'); option.value = value; option.text = value; select.add(option); } }", value);
        await locator.SelectOptionAsync(value);
    }

    public async Task SetCheckedAsync(string selector, bool isChecked)
    {
        var locator = await GetLocatorAsync(selector);
        await locator.SetCheckedAsync(isChecked);
    }

    public async Task<bool> IsCheckedAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        return await locator.IsCheckedAsync();
    }

    public async Task<string?> ReadSelectedOptionLabelAsync(string selector)
    {
        var locator = await GetLocatorAsync(selector);
        return await locator.EvaluateAsync<string?>("select => select.options[select.selectedIndex]?.text ?? null");
    }

    public string CurrentUrl() => _page.Url;

    /// <summary>Waits until the page address differs from <paramref name="previousUrl"/>: a page load or an in-app route change.</summary>
    /// <remarks>
    /// Polls <c>location.href</c> in the page rather than calling <c>WaitForURLAsync</c>. The .NET client checks its cached
    /// URL and then subscribes to navigation events; a client-side route change (Next.js <c>router.push</c>) that lands
    /// between the two is missed, and the wait hangs although the address has already changed.
    /// </remarks>
    public async Task WaitForUrlChangeAsync(string previousUrl)
    {
        await _page.WaitForFunctionAsync("previousUrl => location.href !== previousUrl", previousUrl,
            new PageWaitForFunctionOptions { Timeout = _timeoutMilliseconds });
    }

    /// <summary>Opens the current page address again, as a fresh page load.</summary>
    public async Task ReopenCurrentUrlAsync()
    {
        await _page.GotoAsync(_page.Url);
    }

    public async Task<bool> IsHiddenAsync(string selector)
    {
        var locator = _page.Locator(selector);
        return await locator.CountAsync() == 0;
    }

    /// <summary>Whether an element matching the selector is shown now, without waiting; one in the page but hidden is not.</summary>
    public async Task<bool> IsShownNowAsync(string selector)
    {
        return await _page.Locator(selector).First.IsVisibleAsync();
    }

    private async Task<ILocator> GetLocatorAsync(string selector, LocatorWaitForOptions waitForOptions)
    {
        await LoginIfRedirectedAsync();
        var locator = _page.Locator(selector);
        await locator.WaitForAsync(waitForOptions);

        var count = await locator.CountAsync();

        if (count == 0)
        {
            throw new Exception($"No elements found for selector: {selector}");
        }

        return locator;
    }

    private async Task<ILocator> GetLocatorAsync(string selector)
    {
        var waitForOptions = GetDefaultWaitForOptions();
        return await GetLocatorAsync(selector, waitForOptions);
    }

    private LocatorWaitForOptions GetDefaultWaitForOptions()
    {
        return new LocatorWaitForOptions
        {
            State = WaitForSelectorState.Visible,
            Timeout = _timeoutMilliseconds
        };
    }
}
