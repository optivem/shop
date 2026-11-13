using Microsoft.Playwright;

using Driver.Adapter.Ui.Client.Pages;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Shared.Client.Playwright;

using Common;

using Driver.Port;

using Driver.Port.Dtos.Error;

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

    private const string SinglePageAppRootSelector = "#root";

    private const string LoginFormSelector = "#kc-login";

    public const string AuthenticationRequired = "Authentication is required";

    public const string PermissionDenied = "You do not have permission to perform this action";

    private const string NotAuthorizedSelector = "h1:has-text('Not authorized'):visible";

    private const string NewOrderPath = "/new-order";

    private const string CouponManagementPath = "/admin-coupons";

    private const string ProductPath = "/products/";

    private const string CartPath = "/cart";

    private const string CheckoutPath = "/checkout";

    private const string ProfilePath = "/account";

    private const string ProceedToCheckoutSelector = "a[aria-label='Proceed to Checkout']";



    private readonly string _baseUrl;

    private readonly IBrowser _browser;

    private readonly string? _keycloakBaseUrl;

    private IBrowserContext _context;

    private IPage _page;

    private HomePage _homePage;

    private KeycloakUiLogin? _login;



    private IResponse? _response;

    /// <summary>Set while the browser session is a visitor's, which never logs in.</summary>
    private PlaywrightGateway? _visitorPageClient;



    private MyShopUiClient(string baseUrl, IBrowser browser, IBrowserContext context, IPage page, HomePage homePage, KeycloakUiLogin? login, string? keycloakBaseUrl)

    {

        _baseUrl = baseUrl;

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

        var browser = await BrowserPool.RentAsync(IsHeadless);



        var (context, page, login, homePage) = await OpenSessionAsync(browser, baseUrl, keycloakBaseUrl, TestUser.Customer);

        return new MyShopUiClient(baseUrl, browser, context, page, homePage, login, keycloakBaseUrl);

    }



    /// <summary>
    /// Makes <paramref name="user"/> the logged-in user. Switching discards the browser session (a fresh isolated
    /// context) so the next page open logs in as the new user. No-op without Keycloak or when already that user.
    /// </summary>
    /// <returns>true when the session was replaced, so callers must re-open the home page</returns>
    public async Task<bool> SwitchUserAsync(TestUser user)
    {
        if (_login == null || _login.User.Equals(user))
        {
            return false;
        }

        await _page.CloseAsync();
        await _context.CloseAsync();
        (_context, _page, _login, _homePage) = await OpenSessionAsync(_browser, _baseUrl, _keycloakBaseUrl, user);
        _response = null;
        _visitorPageClient = null;
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



    /// <summary>Opens the catalog, the home page, as the logged-in user.</summary>
    public async Task<CatalogPage> OpenCatalogAsync()
    {
        await OpenHomePageAsync();
        return new CatalogPage(new PlaywrightGateway(_page, _baseUrl, _login));
    }

    /// <summary>Opens the catalog in a fresh session that never logs in.</summary>
    /// <returns>the catalog, or null when the app sends the visitor to the login form instead</returns>
    public async Task<CatalogPage?> OpenCatalogAnonymouslyAsync()
    {
        await _page.CloseAsync();
        await _context.CloseAsync();
        (_context, _page, _login, _homePage) = await OpenSessionAsync(_browser, _baseUrl, _keycloakBaseUrl, _login?.User ?? TestUser.Customer);
        _visitorPageClient = null;
        _response = await _page.GotoAsync(_baseUrl);
        var anonymousPageClient = new PlaywrightGateway(_page, _baseUrl);
        await anonymousPageClient.WaitForVisibleAsync(CatalogPage.ReadySelector + ", " + LoginFormSelector);
        if (_login != null && _login.IsLoginPage(_page))
        {
            return null;
        }
        return new CatalogPage(anonymousPageClient);
    }

    /// <summary>Replaces the browser session with a fresh one that never logs in: the anonymous visitor's.</summary>
    public async Task StartVisitorSessionAsync()
    {
        await _page.CloseAsync();
        await _context.CloseAsync();
        (_context, _page, _login, _homePage) = await OpenSessionAsync(_browser, _baseUrl, _keycloakBaseUrl, _login?.User ?? TestUser.Customer);
        _response = null;
        _visitorPageClient = new PlaywrightGateway(_page, _baseUrl);
    }

    /// <summary>Opens a product by its page address, in the visitor's session if one was started, else as the logged-in user.</summary>
    /// <returns>the product page, or null when the app sends the visitor to the login form instead</returns>
    public async Task<ProductPage?> OpenProductPageAsync(string sku)
    {
        var path = ProductPath + Uri.EscapeDataString(sku);
        if (_visitorPageClient != null)
        {
            _response = await _page.GotoAsync(_baseUrl + path);
            await _visitorPageClient.WaitForVisibleAsync(ProductPage.ReadySelector + ", " + LoginFormSelector);
            if (_login != null && _login.IsLoginPage(_page))
            {
                return null;
            }
            return new ProductPage(_visitorPageClient);
        }
        await OpenHomePageAsync();
        await OpenDirectlyAsync(path, path);
        return new ProductPage(new PlaywrightGateway(_page, _baseUrl, _login));
    }

    /// <summary>Opens the catalog in the visitor's session if one was started, else as the logged-in user.</summary>
    /// <returns>the catalog, or null when the app sends the visitor to the login form instead</returns>
    public async Task<CatalogPage?> OpenCatalogInSessionAsync()
    {
        if (_visitorPageClient == null)
        {
            return await OpenCatalogAsync();
        }
        _response = await _page.GotoAsync(_baseUrl);
        await _visitorPageClient.WaitForVisibleAsync(CatalogPage.ReadySelector + ", " + LoginFormSelector);
        if (_login != null && _login.IsLoginPage(_page))
        {
            return null;
        }
        return new CatalogPage(_visitorPageClient);
    }

    /// <summary>
    /// Leaves the site in the same browser: the page is closed and a new, blank one takes its place, keeping the
    /// browser's cookies and storage. Open a page to come back.
    /// </summary>
    public async Task RevisitAsync()
    {
        await _page.CloseAsync();
        _page = await _context.NewPageAsync();
        _homePage = new HomePage(new PlaywrightGateway(_page, _baseUrl, _login));
        _response = null;
        if (_visitorPageClient != null)
        {
            _visitorPageClient = new PlaywrightGateway(_page, _baseUrl);
        }
    }

    /// <summary>Signs in as <paramref name="user"/> with the header's "Sign In" link, from the page that is open.</summary>
    public async Task SignInAsync(TestUser user)
    {
        var userLogin = KeycloakUiLogin.ForBaseUrl(_keycloakBaseUrl, user)
            ?? throw new InvalidOperationException("Signing in can only be exercised against a system with a login");
        await userLogin.EnsureLoggedInAsync(_page, HomeReadySelector);
    }

    /// <summary>Whether the page is the login form.</summary>
    public bool IsOnLoginPage() => _login != null && _login.IsLoginPage(_page);

    /// <summary>The page that is open, read for what every page shows, such as the header's cart badge.</summary>
    public HomePage CurrentPageInSession()
        => new(_visitorPageClient ?? new PlaywrightGateway(_page, _baseUrl, _login));

    /// <summary>The page client of the current session: the visitor's, if one was started, else the logged-in user's.</summary>
    public PlaywrightGateway SessionPageClient()
        => _visitorPageClient ?? new PlaywrightGateway(_page, _baseUrl, _login);

    /// <summary>
    /// Opens the checkout by its page address in the current session, and waits until the app shows where it sent the
    /// user: the checkout, the cart, the login form or a refusal.
    /// </summary>
    public async Task OpenCheckoutPageAsync()
    {
        if (_visitorPageClient != null)
        {
            _response = await _page.GotoAsync(_baseUrl + CheckoutPath);
        }
        else
        {
            await OpenHomePageAsync();
            await OpenDirectlyAsync(CheckoutPath, CheckoutPath);
        }
        await WaitForCheckoutOutcomeAsync();
    }

    /// <summary>Opens the cart in the current session and follows its "Proceed to checkout", like <see cref="OpenCheckoutPageAsync"/>.</summary>
    public async Task ProceedToCheckoutFromCartAsync()
    {
        var cartPage = await OpenCartPageAsync();
        await cartPage.WaitForLoadedAsync();
        await SessionPageClient().ClickAsync(ProceedToCheckoutSelector);
        await WaitForCheckoutOutcomeAsync();
    }

    /// <summary>Signs in as <paramref name="user"/> on the login form that is open, and waits for the page the app returns to.</summary>
    public async Task SignInOnLoginFormAsync(TestUser user)
    {
        var userLogin = KeycloakUiLogin.ForBaseUrl(_keycloakBaseUrl, user)
            ?? throw new InvalidOperationException("Signing in can only be exercised against a system with a login");
        await userLogin.EnsureLoggedInAsync(_page, CheckoutPage.ReadySelector + ", " + CartPage.ReadySelector + ", " + CatalogPage.ReadySelector);
        // A single-page UI may show its (public) landing page while it still exchanges the login code, and only then
        // move to the page the visitor came from: wait until the code is gone from the address.
        await _page.WaitForURLAsync(url => !System.Text.RegularExpressions.Regex.IsMatch(url, "[?&]code="));
        if (await _page.Locator(CheckoutPage.ReadySelector).CountAsync() == 0 && _page.Url.Contains(CheckoutPath))
        {
            await SessionPageClient().WaitForVisibleAsync(CheckoutPage.ReadySelector);
        }
    }

    /// <summary>Whether the app refused the page: a 403 response (server-rendered UIs) or a "Not authorized" page (React).</summary>
    public async Task<bool> IsRefusedAsync()
        => IsCheckoutForbidden() || await _page.Locator(NotAuthorizedSelector).CountAsync() > 0;

    /// <summary>The path of the page address that is open, such as "/cart".</summary>
    public string CurrentPath() => new Uri(_page.Url).AbsolutePath;

    private bool IsCheckoutForbidden()
        => _response?.Status == (int)HttpStatusCode.Forbidden && _page.Url.Contains(CheckoutPath);

    private async Task WaitForCheckoutOutcomeAsync()
    {
        if (IsCheckoutForbidden())
        {
            return;
        }
        await _page.WaitForSelectorAsync(CheckoutPage.ReadySelector + ", " + CartPage.ReadySelector + ", " + LoginFormSelector
            + ", " + NotAuthorizedSelector);
    }

    /// <summary>Opens the cart in the current session. The app must already be open.</summary>
    public async Task<CartPage> OpenCartPageAsync()
    {
        if (_visitorPageClient != null)
        {
            _response = await _page.GotoAsync(_baseUrl + CartPath);
            return new CartPage(_visitorPageClient);
        }
        await OpenDirectlyAsync(CartPath, CartPath);
        return new CartPage(new PlaywrightGateway(_page, _baseUrl, _login));
    }

    /// <summary>Opens the profile by its page address, as the logged-in user, and waits until it has loaded.</summary>
    public async Task<ProfilePage> OpenProfilePageAsync()
    {
        await OpenHomePageAsync();
        await OpenDirectlyAsync(ProfilePath, ProfilePath);
        var profilePage = new ProfilePage(new PlaywrightGateway(_page, _baseUrl, _login));
        await profilePage.WaitForLoadedAsync();
        return profilePage;
    }

    /// <summary>
    /// Opens the profile by its page address in a fresh visitor session, signs in as <paramref name="user"/> when the
    /// app asks to, and waits for the profile the app returns to.
    /// </summary>
    public async Task<ProfilePage> OpenProfilePageSigningInAsync(TestUser user)
    {
        var userLogin = KeycloakUiLogin.ForBaseUrl(_keycloakBaseUrl, user)
            ?? throw new InvalidOperationException("Signing in can only be exercised against a system with a login");
        await StartVisitorSessionAsync();
        _response = await _page.GotoAsync(_baseUrl + ProfilePath);
        await userLogin.EnsureLoggedInAsync(_page, ProfilePage.ReadySelector);
        // A single-page UI may show its (public) landing page while it still exchanges the login code, and only then
        // move to the page the visitor came from: wait until the code is gone from the address.
        await _page.WaitForURLAsync(url => !System.Text.RegularExpressions.Regex.IsMatch(url, "[?&]code="));
        var profilePage = new ProfilePage(_visitorPageClient!);
        await profilePage.WaitForLoadedAsync();
        return profilePage;
    }

    /// <summary>Opens an order by its URL, not through the order history. The app must already be open.</summary>
    public async Task<OrderDetailsPage> OpenOrderDetailsPageAsync(string? orderNumber)
    {
        var encodedOrderNumber = Uri.EscapeDataString(orderNumber ?? string.Empty);
        await OpenDirectlyAsync($"/order-details?orderNumber={encodedOrderNumber}", $"/order-details/{encodedOrderNumber}");
        return new OrderDetailsPage(new PlaywrightGateway(_page, _baseUrl, _login));
    }

    /// <summary>Opens the new order page by its URL, reporting a refusal. The app must already be open.</summary>
    public Task<Result<NewOrderPage, SystemError>> OpenNewOrderPageAsync()
    {
        var pageClient = new PlaywrightGateway(_page, _baseUrl, _login);
        return OpenRestrictedPageAsync(NewOrderPath, NewOrderPage.ReadySelector, new NewOrderPage(pageClient));
    }

    /// <summary>Opens the coupon management page by its URL, reporting a refusal. The app must already be open.</summary>
    public Task<Result<CouponManagementPage, SystemError>> OpenCouponManagementPageAsync()
    {
        var pageClient = new PlaywrightGateway(_page, _baseUrl, _login);
        return OpenRestrictedPageAsync(CouponManagementPath, CouponManagementPage.ReadySelector, new CouponManagementPage(pageClient));
    }

    /// <summary>Opens <paramref name="path"/> in a throwaway session that never logs in, and requires the app to send it to the login form.</summary>
    public Task AssertLoginRequiredForAsync(string path) => AssertLoginRequiredForAsync(path, path);

    /// <summary>
    /// Like <see cref="AssertLoginRequiredForAsync(string)"/>, for a page whose React route differs from its
    /// server-rendered path: the React frontend matches no route for the server path, so it would never ask to log in.
    /// </summary>
    public async Task AssertLoginRequiredForAsync(string serverPath, string singlePageAppRoute)
    {
        if (_login == null)
        {
            throw new InvalidOperationException("Anonymous access can only be exercised against a system with a login");
        }

        await using var anonymousContext = await _browser.NewContextAsync();
        var anonymousPage = await anonymousContext.NewPageAsync();
        await anonymousPage.GotoAsync(_baseUrl + serverPath);
        if (singlePageAppRoute != serverPath && await anonymousPage.Locator(SinglePageAppRootSelector).CountAsync() > 0)
        {
            await anonymousPage.GotoAsync(_baseUrl + singlePageAppRoute);
        }
        await _login.WaitForLoginFormAsync(anonymousPage);
    }

    /// <summary>The server-rendered UIs refuse with a 403 response; the React frontend renders a "Not authorized" page.</summary>
    private async Task<Result<T, SystemError>> OpenRestrictedPageAsync<T>(string path, string readySelector, T openedPage)
    {
        await OpenDirectlyAsync(path, path);

        if (_response?.Status == (int)HttpStatusCode.Forbidden)
        {
            return SystemResults.Failure<T>(PermissionDenied);
        }

        var pageClient = new PlaywrightGateway(_page, _baseUrl, _login);
        await pageClient.WaitForVisibleAsync(readySelector + ", " + NotAuthorizedSelector);
        return await pageClient.IsHiddenAsync(NotAuthorizedSelector)
            ? SystemResults.Success(openedPage)
            : SystemResults.Failure<T>(PermissionDenied);
    }

    /// <summary>
    /// Server-rendered UIs are simply navigated to. The React frontend keeps its session in memory and returns to
    /// the home page after the login redirect, so a reload would lose the URL: it is moved to the route in place.
    /// </summary>
    private async Task OpenDirectlyAsync(string serverPath, string singlePageAppRoute)
    {
        if (await _page.Locator(SinglePageAppRootSelector).CountAsync() > 0)
        {
            await new PlaywrightGateway(_page, _baseUrl, _login).NavigateWithinAppAsync(singlePageAppRoute);
        }
        else
        {
            _response = await _page.GotoAsync(_baseUrl + serverPath);
        }
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

        // The browser is shared (BrowserPool): hand it to the next test instead of closing it.
        if (_browser != null)

            BrowserPool.Return(_browser);

        GC.SuppressFinalize(this);

    }

}





