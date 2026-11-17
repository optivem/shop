namespace MyCompany.MyShop.Backend.Api;

/// <summary>
/// Refuses to start with <c>APP_PROFILE=prod</c> (set by the production deploy only) when a secret or endpoint is missing
/// or still the committed test default (DB password <c>app</c>, a localhost issuer). Local and pipeline stacks
/// run without the profile, so their test defaults keep working. Parity with the Java backends' prod-profile guard.
/// </summary>
public static class ProductionConfigGuard
{
    public const string ProfileVariable = "APP_PROFILE";
    public const string ProdProfile = "prod";
    public const string RealMode = "real";
    public const string TestDbPassword = "app";

    public static bool IsProdProfile(IConfiguration configuration) => configuration[ProfileVariable] == ProdProfile;

    public static void EnsureSafeForProduction(IConfiguration configuration)
    {
        if (!IsProdProfile(configuration))
        {
            return;
        }
        var problems = Problems(configuration);
        if (problems.Count > 0)
        {
            throw new InvalidOperationException("Refusing to start with the prod profile: " + string.Join("; ", problems));
        }
    }

    public static IReadOnlyList<string> Problems(IConfiguration configuration)
    {
        var problems = new List<string>();
        var dbPassword = configuration["POSTGRES_DB_PASSWORD"];
        if (string.IsNullOrWhiteSpace(dbPassword) || dbPassword == TestDbPassword)
        {
            problems.Add("POSTGRES_DB_PASSWORD is missing or the test default");
        }

        var issuer = configuration["AUTH_ISSUER_URI"] ?? configuration["Auth:IssuerUri"];
        if (string.IsNullOrWhiteSpace(issuer) || IsLocalhost(issuer))
        {
            problems.Add("AUTH_ISSUER_URI is missing or points at localhost");
        }

        // B-PRG 2.3: production reads the local system clock; the external (stub) clock exists so tests control "now".
        var externalSystemMode = configuration["EXTERNAL_SYSTEM_MODE"] ?? configuration["External:SystemMode"];
        if (externalSystemMode != null && externalSystemMode != RealMode)
        {
            problems.Add("EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)");
        }

        return problems;
    }

    private static bool IsLocalhost(string uri) =>
        !Uri.TryCreate(uri, UriKind.Absolute, out var parsed)
        || parsed.Host is "localhost" or "127.0.0.1";
}
