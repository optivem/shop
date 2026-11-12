package com.mycompany.myshop.testkit.driver.adapter.ui.client.pages;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.playwright.PageClient;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The order history `/order-history`: My orders for a customer, All orders for an admin (UC-002, UC-011). An admin also
 * gets a customer filter, which offers the customers found in the order list once they have loaded, and a Customer
 * column. The cells are read by their column's header, which every UI names the same way.
 */
public class OrderHistoryPage extends BasePage {
    private static final String ORDER_NUMBER_INPUT_SELECTOR = "[aria-label='Order Number']";
    private static final String SEARCH_BUTTON_SELECTOR = "[aria-label='Refresh Order List']";
    private static final String CUSTOMER_FILTER_SELECTOR = "select[aria-label='Customer Filter']";
    /** A choice after "All customers": the customers have loaded. */
    private static final String LOADED_CUSTOMER_OPTION_SELECTOR = CUSTOMER_FILTER_SELECTOR + " option:nth-child(2)";
    private static final String CUSTOMER_OPTION_SELECTOR = CUSTOMER_FILTER_SELECTOR + " option";
    private static final String TABLE_ROW_SELECTOR = "table tbody tr:visible";
    private static final String ORDER_NUMBER_CELL_SELECTOR = "table tbody tr:visible:has(a) td:first-child";
    private static final String HEADER_SELECTOR = "table:visible thead th";
    private static final String ORDER_CELL_SELECTOR = "table:visible tbody tr:has(a) td";
    private static final String DETAILS_LINK_SELECTOR = "table:visible tbody tr:has(a) a";
    private static final String EMPTY_MESSAGE_SELECTOR = ".orders-empty:visible";
    private static final String ROW_SELECTOR_TEMPLATE = "//tr[contains(., '%s')]";
    private static final String VIEW_DETAILS_LINK_SELECTOR_TEMPLATE = "%s//a[contains(text(), 'View Details')]";
    private static final String ORDER_NUMBER_HEADER = "Order Number";
    private static final String CUSTOMER_HEADER = "Customer";
    private static final String SKU_HEADER = "SKU";
    private static final String QUANTITY_HEADER = "Quantity";
    private static final String ORDER_DATE_HEADER = "Order Date";
    private static final String TOTAL_PRICE_HEADER = "Total Price";
    /** Every UI shows the order date as {@code toLocaleString('en-US', {timeZone: 'UTC'})}, e.g. "1/10/2026, 9:00:00 AM". */
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss a", Locale.US);
    private static final String DOLLAR_SYMBOL = "$";
    private static final char NARROW_NO_BREAK_SPACE = (char) 0x202F;
    private static final char NO_BREAK_SPACE = (char) 0x00A0;
    private static final String STATUS_HEADER = "Status";
    private static final String NO_CUSTOMER = "-";
    private static final String ALL_CUSTOMERS = "All customers";

    public OrderHistoryPage(PageClient pageClient) {
        super(pageClient);
    }

    public void inputOrderNumber(String orderNumber) {
        pageClient.fill(ORDER_NUMBER_INPUT_SELECTOR, orderNumber);
    }

    public void clickSearch() {
        pageClient.click(SEARCH_BUTTON_SELECTOR);
    }

    public boolean hasCustomerFilter() {
        return !pageClient.isHidden(CUSTOMER_FILTER_SELECTOR);
    }

    /** Chooses this customer (a username), waiting until the filter offers it; the list reloads at once. */
    public void selectCustomer(String username) {
        pageClient.selectOptionByLabel(CUSTOMER_FILTER_SELECTOR, username);
    }

    /** Sets the customer filter back to All customers; the list reloads at once. */
    public void selectAllCustomers() {
        pageClient.selectOptionByLabel(CUSTOMER_FILTER_SELECTOR, ALL_CUSTOMERS);
    }

    /** Waits until the filter offers the customers, then reads its choices as shown. */
    public List<String> readCustomerFilterOptions() {
        pageClient.waitForAttached(LOADED_CUSTOMER_OPTION_SELECTOR);
        return pageClient.readAllTextContentsNoWait(CUSTOMER_OPTION_SELECTOR).stream().map(String::trim).toList();
    }

    public String readCustomerFilterSelection() {
        var label = pageClient.readSelectedOptionLabel(CUSTOMER_FILTER_SELECTOR);
        return label == null ? null : label.trim();
    }

    /** Reads the listed order numbers; the table holds a single linkless row when there are no orders. */
    public List<String> readOrderNumbers() {
        waitForList();
        return pageClient.readAllTextContentsNoWait(ORDER_NUMBER_CELL_SELECTOR).stream().map(String::trim).toList();
    }

    /** Reads the listed orders, each cell by its column's header; the table has a single linkless row when empty. */
    public List<BrowseOrderHistoryResponse.OrderDto> readOrders() {
        waitForList();
        var headers = readHeaders();
        var cells = pageClient.readAllTextContentsNoWait(ORDER_CELL_SELECTOR).stream().map(String::trim).toList();
        var links = pageClient.readAllAttributesNoWait(DETAILS_LINK_SELECTOR, "href");
        var orders = new ArrayList<BrowseOrderHistoryResponse.OrderDto>();
        for (var row = 0; headers.size() * (row + 1) <= cells.size(); row++) {
            var rowCells = cells.subList(headers.size() * row, headers.size() * (row + 1));
            var customer = cell(headers, rowCells, CUSTOMER_HEADER);
            var quantity = cell(headers, rowCells, QUANTITY_HEADER);
            var status = cell(headers, rowCells, STATUS_HEADER);
            var orderDate = cell(headers, rowCells, ORDER_DATE_HEADER);
            var totalPrice = cell(headers, rowCells, TOTAL_PRICE_HEADER);
            orders.add(BrowseOrderHistoryResponse.OrderDto.builder()
                    .orderNumber(cell(headers, rowCells, ORDER_NUMBER_HEADER))
                    .customer(NO_CUSTOMER.equals(customer) ? null : customer)
                    .sku(cell(headers, rowCells, SKU_HEADER))
                    .orderTimestamp(orderDate == null ? null : parseOrderDate(orderDate))
                    .quantity(quantity == null ? 0 : Integer.parseInt(quantity))
                    .totalPrice(totalPrice == null ? null : new BigDecimal(totalPrice.replace(DOLLAR_SYMBOL, "").trim()))
                    .status(status == null ? null : OrderStatus.valueOf(status))
                    .detailsPath(row < links.size() ? links.get(row) : null)
                    .build());
        }
        return orders;
    }

    public boolean hasCustomerColumn() {
        waitForList();
        return readHeaders().contains(CUSTOMER_HEADER);
    }

    /** The message shown instead of orders, such as "No orders found"; null when orders are listed. */
    public String readMessage() {
        waitForList();
        var messages = pageClient.readAllTextContentsNoWait(EMPTY_MESSAGE_SELECTOR);
        return messages.isEmpty() ? null : messages.get(0).trim();
    }

    public boolean isOrderListed(String orderNumber) {
        var rowSelector = getRowSelector(orderNumber);
        return pageClient.isVisible(rowSelector);
    }

    public OrderDetailsPage clickViewOrderDetails(String orderNumber) {
        var rowSelector = getRowSelector(orderNumber);
        var viewDetailsLinkSelector = String.format(VIEW_DETAILS_LINK_SELECTOR_TEMPLATE, rowSelector);
        pageClient.click(viewDetailsLinkSelector);
        return new OrderDetailsPage(pageClient);
    }

    /** The list has loaded: it shows a row, of orders or of its "No orders found" message. */
    private void waitForList() {
        pageClient.readAllTextContents(TABLE_ROW_SELECTOR);
    }

    private List<String> readHeaders() {
        return pageClient.readAllTextContentsNoWait(HEADER_SELECTOR).stream().map(String::trim).toList();
    }

    /** Browsers may put a narrow or a non-breaking space before AM/PM. */
    private static Instant parseOrderDate(String text) {
        var normalized = text.replace(NARROW_NO_BREAK_SPACE, ' ').replace(NO_BREAK_SPACE, ' ').trim();
        return LocalDateTime.parse(normalized, ORDER_DATE_FORMAT).toInstant(ZoneOffset.UTC);
    }

    /** The cell under this header, or null when the list has no such column. */
    private static String cell(List<String> headers, List<String> rowCells, String header) {
        var column = headers.indexOf(header);
        return column < 0 ? null : rowCells.get(column);
    }

    private static String getRowSelector(String orderNumber) {
        return String.format(ROW_SELECTOR_TEMPLATE, orderNumber);
    }
}
