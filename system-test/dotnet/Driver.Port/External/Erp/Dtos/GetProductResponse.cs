namespace Driver.Port.External.Erp.Dtos;

public class GetProductResponse
{
    public required string Sku { get; set; }
    public string? Title { get; set; }
    public string? Description { get; set; }
    public string? Category { get; set; }
    public string? Brand { get; set; }
    public decimal Price { get; set; }
}
