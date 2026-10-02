namespace Driver.Adapter.Shared.Client.Http;

/// <summary>Test-realm users (the realm import defines them; they exist in the test realm only).</summary>
public enum TestUser
{
    Customer,
    Admin
}

public static class TestUserExtensions
{
    public static string Username(this TestUser user) => user switch
    {
        TestUser.Customer => "customer1",
        TestUser.Admin => "admin1",
        _ => throw new ArgumentOutOfRangeException(nameof(user), user, null)
    };

    public static string Password(this TestUser user) => user switch
    {
        TestUser.Customer => "customer1-test-password",
        TestUser.Admin => "admin1-test-password",
        _ => throw new ArgumentOutOfRangeException(nameof(user), user, null)
    };
}
