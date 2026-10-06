using Common;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Api.Client.Dtos.Errors;

using Driver.Port.Dtos;



namespace Driver.Adapter.Api.Client.Controllers;



public class OrderController

{

    private const string Endpoint = "/api/orders";



    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;



    public OrderController(JsonHttpClient<ProblemDetailResponse> httpClient)

    {

        _httpClient = httpClient;

    }



    public Task<Result<PlaceOrderResponse, ProblemDetailResponse>> PlaceOrderAsync(PlaceOrderRequest request)

        => _httpClient.PostAsync<PlaceOrderResponse>(Endpoint, request);



    public Task<Result<ViewOrderResponse, ProblemDetailResponse>> ViewOrderAsync(string? orderNumber)

        => _httpClient.GetAsync<ViewOrderResponse>($"{Endpoint}/{orderNumber}");



    public Task<Result<BrowseOrderHistoryResponse, ProblemDetailResponse>> BrowseOrderHistoryAsync(string? orderNumber)
        => _httpClient.GetAsync<BrowseOrderHistoryResponse>(string.IsNullOrWhiteSpace(orderNumber)
            ? Endpoint
            : $"{Endpoint}?orderNumber={Uri.EscapeDataString(orderNumber)}");

    public Task<Result<VoidValue, ProblemDetailResponse>> CancelOrderAsync(string? orderNumber)
        => _httpClient.PostAsync($"{Endpoint}/{orderNumber}/cancel");

    public Task<Result<VoidValue, ProblemDetailResponse>> DeliverOrderAsync(string? orderNumber)
        => _httpClient.PostAsync($"{Endpoint}/{orderNumber}/deliver");

}









