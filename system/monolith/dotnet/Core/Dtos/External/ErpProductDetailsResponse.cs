namespace MyCompany.MyShop.Monolith.Core.Dtos.External;

public class ErpProductDetailsResponse
{
    public string Id { get; set; } = null!;
    public string? Title { get; set; }
    public string? Description { get; set; }
    public decimal Price { get; set; }
    public string? Category { get; set; }
    public string? Brand { get; set; }
    // Where the ERP serves the product photo, as a path on the ERP (e.g. /erp/images/HP-15.jpg); null when it has none.
    public string? ImageUrl { get; set; }
}
