namespace Driver.Adapter.Api.Client;

public enum ApiIdentity
{
    /// <summary>Admin for admin-only operations, customer otherwise.</summary>
    Default,

    /// <summary>Call without any Authorization header.</summary>
    Anonymous,

    Customer,

    Admin
}
