namespace Driver.Port.Dtos;

/// <summary>The address an order is shipped to; its country is the order's tax country.</summary>
public class ShippingAddress
{
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Street { get; set; }
    public string? PostalCode { get; set; }
    public string? City { get; set; }
    public string? Country { get; set; }
}
