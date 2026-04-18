namespace Driver.Adapter.External.Tax.Client.Dtos;

public class ExtGetCountryResponse
{
    public required string Id { get; set; }
    public required string CountryName { get; set; }
    public decimal TaxRate { get; set; }
}
