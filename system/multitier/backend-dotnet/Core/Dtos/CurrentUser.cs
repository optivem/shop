namespace MyCompany.MyShop.Backend.Core.Dtos;

/// <summary>
/// The authenticated caller: token subject, display username, whether they hold the ADMIN role, and the
/// account's email and given / family name (each null when the token does not carry it).
/// </summary>
public record CurrentUser(
    string Subject,
    string? Username,
    bool Admin,
    string? Email = null,
    string? FirstName = null,
    string? LastName = null);
