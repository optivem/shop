namespace Driver.Adapter.Shared.Client.Http;

/// <summary>Test-realm users (the realm import defines them; they exist in the test realm only).</summary>
public enum TestUser
{
    Customer,
    Customer2,
    Admin
}

public static class TestUserExtensions
{
    /// <summary>The test customer at the given zero-based position (0 = customer1).</summary>
    public static TestUser CustomerAt(int index) => index switch
    {
        0 => TestUser.Customer,
        1 => TestUser.Customer2,
        _ => throw new ArgumentOutOfRangeException(nameof(index), index, $"No test customer at index {index}")
    };

    public static string Username(this TestUser user) => user switch
    {
        TestUser.Customer => "customer1",
        TestUser.Customer2 => "customer2",
        TestUser.Admin => "admin1",
        _ => throw new ArgumentOutOfRangeException(nameof(user), user, null)
    };

    public static string Password(this TestUser user) => user switch
    {
        TestUser.Customer => "customer1-test-password",
        TestUser.Customer2 => "customer2-test-password",
        TestUser.Admin => "admin1-test-password",
        _ => throw new ArgumentOutOfRangeException(nameof(user), user, null)
    };
}
