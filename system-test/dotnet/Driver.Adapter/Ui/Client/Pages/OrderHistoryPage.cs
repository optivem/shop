using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Shared.Client.Playwright;

using Driver.Port.Dtos;



namespace Driver.Adapter.Ui.Client.Pages;



public class OrderHistoryPage : BasePage

{

    private const string OrderNumberInputSelector = "[aria-label='Order Number']";

    private const string SearchButtonSelector = "[aria-label='Refresh Order List']";

    private const string TableRowSelector = "table tbody tr:visible";

    private const string OrderNumberCellSelector = "table tbody tr:visible:has(a) td:first-child";



    private const string RowSelectorTemplate = "//tr[contains(., '{0}')]";

    private const string ViewDetailsLinkSelectorTemplate = "{0}//a[contains(text(), 'View Details')]";



    public OrderHistoryPage(PageClient pageClient) : base(pageClient)

    {

    }



    public async Task InputOrderNumberAsync(string? orderNumber)

    {

        await PageClient.FillAsync(OrderNumberInputSelector, orderNumber);

    }



    public async Task ClickSearchAsync()

    {

        await PageClient.ClickAsync(SearchButtonSelector);

    }



    /// <summary>Reads the listed order numbers; the table holds a single linkless row when there are no orders.</summary>
    public async Task<List<string>> ReadOrderNumbersAsync()
    {
        await PageClient.ReadAllTextContentsAsync(TableRowSelector);
        var orderNumbers = await PageClient.ReadAllTextContentsNoWaitAsync(OrderNumberCellSelector);
        return orderNumbers.Select(orderNumber => orderNumber.Trim()).ToList();
    }

    public async Task<bool> WaitForOrderRowAsync(string? orderNumber, int timeoutMilliseconds = 10000)

    {

        try

        {

            var rowSelector = GetRowSelector(orderNumber);

            await PageClient.WaitForVisibleAsync(rowSelector, timeoutMilliseconds);

            return true;

        }

        catch

        {

            return false;

        }

    }



    public async Task<bool> IsOrderListedAsync(string? orderNumber)

    {

        var rowSelector = GetRowSelector(orderNumber);

        return await PageClient.IsVisibleAsync(rowSelector);

    }



    public async Task<OrderDetailsPage> ClickViewOrderDetailsAsync(string? orderNumber)

    {

        var rowSelector = GetRowSelector(orderNumber);

        var viewDetailsLinkSelector = string.Format(ViewDetailsLinkSelectorTemplate, rowSelector);

        await PageClient.ClickAsync(viewDetailsLinkSelector);

        return new OrderDetailsPage(PageClient);

    }



    private static string GetRowSelector(string? orderNumber)

    {

        return string.Format(RowSelectorTemplate, orderNumber);

    }

}









