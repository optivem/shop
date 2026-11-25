namespace Driver.Adapter.Api.Client;

/// <summary>Who the API client calls as, short of a given user (<see cref="MyShopApiClient.AsUser"/>).</summary>
public enum ApiIdentity
{
    /// <summary>Call without any Authorization header.</summary>
    Anonymous,

    /// <summary>The shared test customer customer1.</summary>
    Customer
}
