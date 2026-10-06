using Microsoft.Playwright;

using Driver.Adapter.Ui.Client.Pages;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Shared.Client.Playwright;

using System.Net;

using PlaywrightGateway = Driver.Adapter.Shared.Client.Playwright.PageClient;



namespace Driver.Adapter.Ui.Client;



public class MyShopUiClient : IAsyncDisposable

{

    // Default: headless mode (browser not visible)

    // To see browser during debugging, set: HEADED=true or PLAYWRIGHT_HEADED=true

    private static readonly bool IsHeadless = Environment.GetEnvironmentVariable("HEADED") != "true";



    private const string ContentType = "content-type";

    private const string TextHtml = "text/html";

    private const string HtmlOpeningTag = "<html";

    private const string HtmlClosingTag = "</html>";

    private const string HomeReadySelector = "a[href='/order-history']";



    private readonly string _baseUrl;

    private readonly IPlaywright _playwright;

    private readonly IBrowser _browser;

    private readonly string? _keycloakBaseUrl;

    private IBrowserContext _context;

    private IPage _page;

    private HomePage _homePage;

    private KeycloakUiLogin? _login;



    private IResponse? _response;



    private MyShopUiClient(string baseUrl, IPlaywright playwright, IBrowser browser, IBrowserContext context, IPage page, HomePage homePage, KeycloakUiLogin? login, string? keycloakBaseUrl)

    {

        _baseUrl = baseUrl;

        _playwright = playwright;

        _browser = browser;

        _context = context;

        _page = page;

        _homePage = homePage;

        _login = login;
        _keycloakBaseUrl = keycloakBaseUrl;

    }



    /// <param name="keycloakBaseUrl">When null or blank, no login is performed.</param>
    public static async Task<MyShopUiClient> CreateAsync(string baseUrl, string? keycloakBaseUrl = null)

    {

        var playwright = await Playwright.CreateAsync();

        var browser = await playwright.Chromium.LaunchAsync(new BrowserTypeLaunchOptions { Headless = IsHeadless });



        var (context, page, login, homePage) = await OpenSessionAsync(browser, baseUrl, keycloakBaseUrl, TestUser.Customer);

        return new MyShopUiClient(baseUrl, playwright, browser, context, page, homePage, login, keycloakBaseUrl);

    }



    /// <summary>
    /// Makes <paramref name="user"/> the logged-in user. Switching discards the browser session (a fresh isolated
    /// context) so the next page open logs in as the new user. No-op without Keycloak or when already that user.
    /// </summary>
    /// <returns>true when the session was replaced, so callers must re-open the home page</returns>
    public async Task<bool> SwitchUserAsync(TestUser user)
    {
        if (_login == null || _login.User == user)
        {
            return false;
        }

        await _page.CloseAsync();
        await _context.CloseAsync();
        (_context, _page, _login, _homePage) = await OpenSessionAsync(_browser, _baseUrl, _keycloakBaseUrl, user);
        _response = null;
        return true;
    }

    private static async Task<(IBrowserContext Context, IPage Page, KeycloakUiLogin? Login, HomePage HomePage)> OpenSessionAsync(
        IBrowser browser, string baseUrl, string? keycloakBaseUrl, TestUser user)
    {
        // Create isolated browser context with specific configuration
        var contextOptions = new BrowserNewContextOptions
        {
            ViewportSize = new ViewportSize { Width = 1920, Height = 1080 },
            StorageStatePath = null // Ensure complete isolation between parallel tests
        };
        var context = await browser.NewContextAsync(contextOptions);

        // Each test gets its own page
        var page = await context.NewPageAsync();

        var login = KeycloakUiLogin.ForBaseUrl(keycloakBaseUrl, user);
        var pageClient = new PlaywrightGateway(page, baseUrl, login);
        var homePage = new HomePage(pageClient);

        return (context, page, login, homePage);
    }

    public async Task<HomePage> OpenHomePageAsync()

    {

        _response = await _page.GotoAsync(_baseUrl);

        if (_login != null)
        {
            await _login.EnsureLoggedInAsync(_page, HomeReadySelector);
        }


        return _homePage;

    }



    public bool IsStatusOk()

    {

        return _response?.Status == ((int)HttpStatusCode.OK);

    }



    public async Task<bool> IsPageLoadedAsync()

    {

        if (_response == null || _response.Status != ((int)HttpStatusCode.OK))

        {

            return false;

        }



        var contentType = _response.Headers.TryGetValue(ContentType, out var contentTypeValue) ? contentTypeValue : null;

        if (contentType == null || !contentType.StartsWith(TextHtml))

        {

            return false;

        }



        var pageContent = await _page.ContentAsync();

        return pageContent != null &&

               pageContent.Contains(HtmlOpeningTag) &&

               pageContent.Contains(HtmlClosingTag);

    }



    public async ValueTask DisposeAsync()

    {

        if (_page != null)

            await _page.CloseAsync();

        if (_context != null)

            await _context.CloseAsync();

        if (_browser != null)

            await _browser.CloseAsync();



        _playwright?.Dispose();

        GC.SuppressFinalize(this);

    }

}





