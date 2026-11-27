namespace MyCompany.MyShop.Monolith.Core.Dtos.External;

/// <summary>A product photo as the ERP serves it: the raw bytes and their content type (e.g. image/jpeg).</summary>
public record ErpProductImage(byte[] Content, string ContentType);
