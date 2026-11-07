namespace Driver.Port.External.Erp.Dtos;

public class ReturnsProductsRequest
{
    public List<ReturnsProductRequest> Products { get; set; } = new();
}
