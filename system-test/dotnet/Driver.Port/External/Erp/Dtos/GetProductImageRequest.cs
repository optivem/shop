namespace Driver.Port.External.Erp.Dtos;

public class GetProductImageRequest
{
    /// <summary>The image URL as the ERP gave it on the product, resolved against the ERP base URL.</summary>
    public string? ImageUrl { get; set; }
}
