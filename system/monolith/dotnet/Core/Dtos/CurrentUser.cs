namespace MyCompany.MyShop.Monolith.Core.Dtos;

/// <summary>
/// The authenticated caller: token subject, display username, whether they hold the ADMIN role, and the
/// account's email, given / family name and the shipping country chosen at sign-up (each null when the token does
/// not carry it).
/// </summary>
public record CurrentUser(
    string Subject,
    string? Username,
    bool Admin,
    string? Email = null,
    string? FirstName = null,
    string? LastName = null,
    string? Country = null);
