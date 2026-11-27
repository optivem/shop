namespace Driver.Adapter.External.Erp.Client.Dtos;

public class ExtProductDetailsResponse
{
    public required string Id { get; set; }
    public required string Title { get; set; }
    public required string Description { get; set; }
    public decimal Price { get; set; }
    public required string Category { get; set; }
    public required string Brand { get; set; }
    public string? ImageUrl { get; set; }
}

