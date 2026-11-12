namespace Driver.Port.Dtos;

/// <summary>Views the acting user's own profile; the identity is the only input, apart from a visitor's sign-in (UI only).</summary>
public class ViewProfileRequest
{
    /// <summary><see cref="WhenAskedToSignIn"/>: a visitor who is asked to sign in signs in as the customer (UI only).</summary>
    public const string SignIn = "sign-in";

    /// <summary>What a visitor does when the profile asks to sign in: <see cref="SignIn"/>, or nothing (unset).</summary>
    public string? WhenAskedToSignIn { get; set; }
}
