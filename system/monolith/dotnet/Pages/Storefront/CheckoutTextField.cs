namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>The model of one text input of the checkout address form; <see cref="Error"/> is null when the value is fine.</summary>
public sealed record CheckoutTextField(string Field, string Label, string AriaLabel, string? Value, string ColClass, string? Error);
