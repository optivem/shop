using Dsl.Port.Given;

namespace SystemTests.Latest.AcceptanceTests.Base;

/// <summary>
/// UC-009's Background: the same ERP product list as UC-006's, and the countries' tax rates US 0.00, DE 0.19 and
/// GB 0.20. Checkout offers only those real country codes (no aliases), so their rates are the tax stub's own
/// mappings for US, DE and GB rather than staged per scenario.
/// </summary>
public abstract class BaseCheckOutTest : BaseManageCartTest
{
    protected static readonly Address AnaNovakBerlin = new("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE");
    protected static readonly Address JohnSmithNewYork = new("John", "Smith", "1 Main St", "10001", "New York", "US");
    protected static readonly Address JaneBrownLondon = new("Jane", "Brown", "1 High St", "SW1A 1AA", "London", "GB");
    protected static readonly Address MarieDupontParis = new("Marie", "Dupont", "1 Rue de Rivoli", "75001", "Paris", "FR");

    /// <summary>Given the catalog, the user is logged in as a customer whose saved shipping address is this one.</summary>
    protected IGivenStage GivenTheCatalogAndSavedAddress(Channel channel, Address address)
        => GivenTheCatalog(channel)
            .And().Profile()
            .WithFirstName(address.FirstName)
            .WithLastName(address.LastName)
            .WithStreet(address.Street)
            .WithPostalCode(address.PostalCode)
            .WithCity(address.City)
            .WithCountry(address.Country)
            .SavedByCustomer()
            .And().LoggedInAsCustomer();

    /// <summary>A shipping address, written in the specs as "Ana Novak, Main St 1, 10115 Berlin, DE".</summary>
    public record Address(string FirstName, string LastName, string Street, string PostalCode, string City, string Country)
    {
        public string Formatted() => $"{FirstName} {LastName}, {Street}, {PostalCode} {City}, {Country}";

        /// <summary>This address with one field (as the specs name it, e.g. "postal code") left empty.</summary>
        public Address WithEmpty(string field) => field switch
        {
            "first name" => this with { FirstName = "" },
            "last name" => this with { LastName = "" },
            "street" => this with { Street = "" },
            "postal code" => this with { PostalCode = "" },
            "city" => this with { City = "" },
            "country" => this with { Country = "" },
            _ => throw new ArgumentException($"Unknown address field: {field}", nameof(field))
        };
    }
}
