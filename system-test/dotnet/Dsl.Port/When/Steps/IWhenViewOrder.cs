using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

public interface IWhenViewOrder : IWhenStep
{
    IWhenViewOrder WithOrderNumber(string? orderNumber);

    /// <summary>A visitor who is asked to sign in signs in as the customer, and is then shown the order (UI only).</summary>
    IWhenViewOrder WithSignInWhenAsked();
}
