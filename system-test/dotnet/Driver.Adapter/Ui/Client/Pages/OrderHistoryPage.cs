using Common.Domain;
using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>
/// The order history `/order-history`: My orders for a customer, All orders for an admin (UC-002, UC-011). An admin also
/// gets a customer filter, which offers the customers found in the order list once they have loaded, and a Customer
/// column. The cells are read by their column's header, which every UI names the same way.
/// </summary>
public class OrderHistoryPage : BasePage
{
    private const string OrderNumberInputSelector = "[aria-label='Order Number']";
    private const string SearchButtonSelector = "[aria-label='Refresh Order List']";
    private const string CustomerFilterSelector = "select[aria-label='Customer Filter']";
    /// <summary>A choice after "All customers": the customers have loaded.</summary>
    private const string LoadedCustomerOptionSelector = CustomerFilterSelector + " option:nth-child(2)";
    private const string CustomerOptionSelector = CustomerFilterSelector + " option";
    private const string TableRowSelector = "table tbody tr:visible";
    private const string OrderNumberCellSelector = "table tbody tr:visible:has(a) td:first-child";
    private const string HeaderSelector = "table:visible thead th";
    private const string OrderCellSelector = "table:visible tbody tr:has(a) td";
    private const string DetailsLinkSelector = "table:visible tbody tr:has(a) a";
    private const string EmptyMessageSelector = ".orders-empty:visible";
    private const string RowSelectorTemplate = "//tr[contains(., '{0}')]";
    private const string ViewDetailsLinkSelectorTemplate = "{0}//a[contains(text(), 'View Details')]";
    private const string OrderNumberHeader = "Order Number";
    private const string CustomerHeader = "Customer";
    private const string SkuHeader = "SKU";
    private const string QuantityHeader = "Quantity";
    private const string StatusHeader = "Status";
    private const string NoCustomer = "-";
    private const string AllCustomers = "All customers";

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

    public async Task<bool> HasCustomerFilterAsync() => !await PageClient.IsHiddenAsync(CustomerFilterSelector);

    /// <summary>Chooses this customer (a username), waiting until the filter offers it; the list reloads at once.</summary>
    public Task SelectCustomerAsync(string username) => PageClient.SelectOptionByLabelAsync(CustomerFilterSelector, username);

    /// <summary>Sets the customer filter back to All customers; the list reloads at once.</summary>
    public Task SelectAllCustomersAsync() => PageClient.SelectOptionByLabelAsync(CustomerFilterSelector, AllCustomers);

    /// <summary>Waits until the filter offers the customers, then reads its choices as shown.</summary>
    public async Task<List<string>> ReadCustomerFilterOptionsAsync()
    {
        await PageClient.WaitForAttachedAsync(LoadedCustomerOptionSelector);
        return (await PageClient.ReadAllTextContentsNoWaitAsync(CustomerOptionSelector)).Select(option => option.Trim()).ToList();
    }

    public async Task<string?> ReadCustomerFilterSelectionAsync()
        => (await PageClient.ReadSelectedOptionLabelAsync(CustomerFilterSelector))?.Trim();

    /// <summary>Reads the listed order numbers; the table holds a single linkless row when there are no orders.</summary>
    public async Task<List<string>> ReadOrderNumbersAsync()
    {
        await WaitForListAsync();
        var orderNumbers = await PageClient.ReadAllTextContentsNoWaitAsync(OrderNumberCellSelector);
        return orderNumbers.Select(orderNumber => orderNumber.Trim()).ToList();
    }

    /// <summary>Reads the listed orders, each cell by its column's header; the table has a single linkless row when empty.</summary>
    public async Task<List<BrowseOrderHistoryResponse.OrderDto>> ReadOrdersAsync()
    {
        await WaitForListAsync();
        var headers = await ReadHeadersAsync();
        var cells = (await PageClient.ReadAllTextContentsNoWaitAsync(OrderCellSelector)).Select(cell => cell.Trim()).ToList();
        var links = await PageClient.ReadAllAttributesNoWaitAsync(DetailsLinkSelector, "href");
        var orders = new List<BrowseOrderHistoryResponse.OrderDto>();
        for (var row = 0; headers.Count > 0 && headers.Count * (row + 1) <= cells.Count; row++)
        {
            var rowCells = cells.GetRange(headers.Count * row, headers.Count);
            var customer = Cell(headers, rowCells, CustomerHeader);
            var quantity = Cell(headers, rowCells, QuantityHeader);
            var status = Cell(headers, rowCells, StatusHeader);
            orders.Add(new BrowseOrderHistoryResponse.OrderDto
            {
                OrderNumber = Cell(headers, rowCells, OrderNumberHeader),
                Customer = customer == NoCustomer ? null : customer,
                Sku = Cell(headers, rowCells, SkuHeader),
                Quantity = quantity == null ? 0 : int.Parse(quantity),
                Status = status == null ? default : Enum.Parse<OrderStatus>(status, ignoreCase: true),
                DetailsPath = row < links.Count ? links[row] : null,
            });
        }
        return orders;
    }

    public async Task<bool> HasCustomerColumnAsync()
    {
        await WaitForListAsync();
        return (await ReadHeadersAsync()).Contains(CustomerHeader);
    }

    /// <summary>The message shown instead of orders, such as "No orders found"; null when orders are listed.</summary>
    public async Task<string?> ReadMessageAsync()
    {
        await WaitForListAsync();
        var messages = await PageClient.ReadAllTextContentsNoWaitAsync(EmptyMessageSelector);
        return messages.Count == 0 ? null : messages[0].Trim();
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

    /// <summary>The list has loaded: it shows a row, of orders or of its "No orders found" message.</summary>
    private Task WaitForListAsync() => PageClient.ReadAllTextContentsAsync(TableRowSelector);

    private async Task<List<string>> ReadHeadersAsync()
        => (await PageClient.ReadAllTextContentsNoWaitAsync(HeaderSelector)).Select(header => header.Trim()).ToList();

    /// <summary>The cell under this header, or null when the list has no such column.</summary>
    private static string? Cell(List<string> headers, List<string> rowCells, string header)
    {
        var column = headers.IndexOf(header);
        return column < 0 ? null : rowCells[column];
    }

    private static string GetRowSelector(string? orderNumber)
    {
        return string.Format(RowSelectorTemplate, orderNumber);
    }
}
