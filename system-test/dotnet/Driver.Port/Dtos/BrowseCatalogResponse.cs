namespace Driver.Port.Dtos;

/// <summary>The catalog as shown. The filter, sort and message fields are UI-only and stay null on the API channel.</summary>
public class BrowseCatalogResponse
{
    public List<ProductDto>? Products { get; set; }
    public int? ProductCount { get; set; }

    /// <summary>The category filter's options, "All" included.</summary>
    public List<string>? Categories { get; set; }
    public string? SelectedCategory { get; set; }
    public List<string>? Brands { get; set; }
    public List<string>? TickedBrands { get; set; }
    public string? Sort { get; set; }
    public string? Message { get; set; }
    public bool? ClearFiltersOffered { get; set; }

    public class ProductDto
    {
        public string? Sku { get; set; }
        public string? Title { get; set; }
        public string? Brand { get; set; }
        public string? Category { get; set; }
        public string? Description { get; set; }

        /// <summary>The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API.</summary>
        public string? Price { get; set; }
    }
}
