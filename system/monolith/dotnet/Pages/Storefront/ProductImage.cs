namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// The model of the product image partial. <paramref name="ImageUrl"/> is the shop's own URL for the ERP photo
/// (null when the ERP has none: the partial then shows the neutral placeholder). <paramref name="Alt"/> is what the
/// photo shows (the product title), empty for a thumbnail that sits next to the product's own title.
/// <paramref name="Shape"/> is "card" (4:3, the photos are 600x450) or "square"; <paramref name="Lazy"/> lazy-loads
/// the photo (catalog grids and thumbnails; the product page loads its one photo straight away).
/// </summary>
public sealed record ProductImage(string? ImageUrl, string Alt, string Shape = "card", bool Lazy = false);
