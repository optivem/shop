namespace MyCompany.MyShop.Backend.Core.Dtos.External;

public class ErpProductDetailsResponse
{
    public string Id { get; set; } = null!;
    public string? Title { get; set; }
    public string? Description { get; set; }
    public decimal Price { get; set; }
    public string? Category { get; set; }
    public string? Brand { get; set; }
}
