using Microsoft.Playwright;

using Driver.Adapter.Shop.Ui.Client.Pages;

using Driver.Adapter.Shared.Client.Playwright;

using System.Net;

using PlaywrightGateway = Driver.Adapter.Shared.Client.Playwright.PageClient;



namespace Driver.Adapter.Shop.Ui.Client;



public class ShopUiClient : IAsyncDisposable

{

    // Default: headless mode (browser not visible)

    // To see browser during debugging, set: HEADED=true or PLAYWRIGHT_HEADED=true

    private static readonly bool IsHeadless = Environment.GetEnvironmentVariable("HEADED") != "true";



    private const string ContentType = "content-type";

    private const string TextHtml = "text/html";

    private const string HtmlOpeningTag = "<html";

    private const string HtmlClosingTag = "</html>";



    private readonly string _baseUrl;

    private readonly IPlaywright _playwright;

    private readonly IBrowser _browser;

    private readonly IBrowserContext _context;

    private readonly IPage _page;

    private readonly HomePage _homePage;



    private IResponse? _response;



    private ShopUiClient(string baseUrl, IPlaywright playwright, IBrowser browser, IBrowserContext context, IPage page, HomePage homePage)

    {

        _baseUrl = baseUrl;

        _playwright = playwright;

        _browser = browser;

        _context = context;

        _page = page;

        _homePage = homePage;

    }



    public static async Task<ShopUiClient> CreateAsync(string baseUrl)

    {

        var playwright = await Playwright.CreateAsync();

        var browser = await playwright.Chromium.LaunchAsync(new BrowserTypeLaunchOptions { Headless = IsHeadless });



        // Create isolated browser context with specific configuration

        var contextOptions = new BrowserNewContextOptions

        {

            ViewportSize = new ViewportSize { Width = 1920, Height = 1080 },

            StorageStatePath = null // Ensure complete isolation between parallel tests

        };

        var context = await browser.NewContextAsync(contextOptions);



        // Each test gets its own page

        var page = await context.NewPageAsync();

        var pageClient = new PlaywrightGateway(page, baseUrl);

        var homePage = new HomePage(pageClient);



        return new ShopUiClient(baseUrl, playwright, browser, context, page, homePage);

    }



    public async Task<HomePage> OpenHomePageAsync()

    {

        _response = await _page.GotoAsync(_baseUrl);

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



        var contentType = _response.Headers.ContainsKey(ContentType) ? _response.Headers[ContentType] : null;

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

    }

}





