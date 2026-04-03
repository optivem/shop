using Common;

using Driver.Adapter.Shared.Client.WireMock;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.External.Erp.Client.Dtos;

using Driver.Adapter.External.Erp.Client.Dtos.Error;



namespace Driver.Adapter.External.Erp.Client;



public class ErpStubClient : BaseErpClient

{

    private const string ErpProductsEndpoint = "/erp/api/products";



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



    public Task RemoveStubsAsync() => _wireMockClient.RemoveStubsAsync();

}





