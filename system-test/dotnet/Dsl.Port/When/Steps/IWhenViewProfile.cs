using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>Views the logged-in user's own profile.</summary>
public interface IWhenViewProfile : IWhenStep
{
    /// <summary>A visitor who is asked to sign in signs in as the customer, and is then shown the profile (UI only).</summary>
    IWhenViewProfile WithSignInWhenAsked();
}
