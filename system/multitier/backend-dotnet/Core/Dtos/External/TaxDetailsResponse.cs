namespace Optivem.Shop.Backend.Core.Dtos.External;

public class TaxDetailsResponse
{
    public string Id { get; set; } = null!;
    public string CountryName { get; set; } = null!;
    public decimal TaxRate { get; set; }
}
