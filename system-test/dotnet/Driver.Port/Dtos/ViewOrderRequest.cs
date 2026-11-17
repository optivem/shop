namespace Driver.Port.Dtos;

public class ViewOrderRequest
{
    /// <summary><see cref="WhenAskedToSignIn"/>: a visitor who is asked to sign in signs in as the customer (UI only).</summary>
    public const string SignIn = "sign-in";

    public string? OrderNumber { get; set; }

    /// <summary>What a visitor does when the order details ask to sign in: <see cref="SignIn"/>, or nothing (unset).</summary>
    public string? WhenAskedToSignIn { get; set; }
}
