namespace Driver.Port;

/// <summary>A sign-in account (a customer's or an admin's) created for one scenario: what the drivers sign in with.</summary>
public sealed record UserAccount(string Username, string Password)
{
    public override string ToString() => $"UserAccount {{ Username = {Username} }}";
}
