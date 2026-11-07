namespace Driver.Port.External.Erp.Dtos;

public class GetProductsResponse
{
    public List<GetProductResponse> Products { get; set; } = new();
}
