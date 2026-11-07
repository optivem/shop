namespace Driver.Port.External.Erp.Dtos;

public class ReturnsProductRequest
{
    public string? Sku { get; set; }
    public string? Price { get; set; }

    /// <summary>Optional; when null the driver falls back to its default product details.</summary>
    public string? Title { get; set; }
    public string? Description { get; set; }
    public string? Category { get; set; }
    public string? Brand { get; set; }
}
