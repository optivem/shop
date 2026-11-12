namespace Driver.Port;

/// <summary>A customer sign-in account created for one scenario: what the drivers sign in with.</summary>
public sealed record CustomerAccount(string Username, string Password)
{
    public override string ToString() => $"CustomerAccount {{ Username = {Username} }}";
}
