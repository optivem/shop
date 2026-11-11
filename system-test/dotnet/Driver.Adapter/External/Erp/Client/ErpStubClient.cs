using Common;

using Driver.Adapter.Shared.Client.WireMock;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.External.Erp.Client.Dtos;

using Driver.Adapter.External.Erp.Client.Dtos.Error;



namespace Driver.Adapter.External.Erp.Client;



public class ErpStubClient : BaseErpClient

{

    private const string ErpProductsEndpoint = "/erp/api/products";
    private const string ErpPromotionEndpoint = "/erp/api/promotion";
    private const string ErpApiPattern = "/erp/api/.*";



    private readonly JsonWireMockClient _wireMockClient;



    public ErpStubClient(string baseUrl) : base(baseUrl)

    {

        _wireMockClient = new JsonWireMockClient(baseUrl);

    }



    public new void Dispose()

    {

        base.Dispose();

        _wireMockClient?.Dispose();

    }



    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetProductAsync(ExtProductDetailsResponse response)

        => _wireMockClient.StubGetAsync($"{ErpProductsEndpoint}/{response.Id}", HttpStatus.Ok, response)

            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetProductsAsync(List<ExtProductDetailsResponse> response)
        => _wireMockClient.StubGetAsync(ErpProductsEndpoint, HttpStatus.Ok, response)
            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureGetPromotionAsync(ExtGetPromotionResponse response)

        => _wireMockClient.StubGetAsync(ErpPromotionEndpoint, HttpStatus.Ok, response)

            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Every ERP API call answers 503, outranking any product or promotion stubs.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> ConfigureUnavailableAsync()

        => _wireMockClient.StubGetMatchingAsync(ErpApiPattern, HttpStatus.ServiceUnavailable)

            .MapErrorAsync(ExtErpErrorResponse.From);

    /// <summary>Removes the outage stub, so the product and promotion stubs answer again.</summary>
    public Task<Result<VoidValue, ExtErpErrorResponse>> RemoveUnavailableAsync()
        => _wireMockClient.RemoveStubsMatchingAsync(ErpApiPattern)
            .MapErrorAsync(ExtErpErrorResponse.From);

    public Task RemoveStubsAsync() => _wireMockClient.RemoveStubsAsync();

}





