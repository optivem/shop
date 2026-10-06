namespace MyCompany.MyShop.Backend.Core.Dtos;

/// <summary>The authenticated caller: token subject, display username, and whether they hold the ADMIN role.</summary>
public record CurrentUser(string Subject, string? Username, bool Admin);
