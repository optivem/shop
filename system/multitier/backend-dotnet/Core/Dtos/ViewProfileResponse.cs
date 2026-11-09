namespace MyCompany.MyShop.Backend.Core.Dtos;

/// <summary>
/// The caller's profile. The email always comes from the sign-in account; without a saved profile the
/// names come from the account and the address fields are null.
/// </summary>
public class ViewProfileResponse
{
    public string? FirstName { get; set; }
    public string? LastName { get; set; }
    public string? Email { get; set; }
    public string? Street { get; set; }
    public string? PostalCode { get; set; }
    public string? City { get; set; }
    public string? Country { get; set; }
}
