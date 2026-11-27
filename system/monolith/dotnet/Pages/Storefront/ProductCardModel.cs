using MyCompany.MyShop.Monolith.Core.Dtos;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// The model of the product card partial. The catalog grid's cards carry the UI drivers' hooks (aria-labels,
/// data-testids); the home page's Bestsellers row reuses the card without them, so each hook still names one element.
/// </summary>
public sealed record ProductCardModel(ViewProductDetailsResponse Product, bool TestHooks = true);
