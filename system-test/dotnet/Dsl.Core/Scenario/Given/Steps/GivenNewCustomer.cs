using Dsl.Port.Given.Steps;
using Driver.Port;
using static Dsl.Core.Scenario.ScenarioDefaults;

namespace Dsl.Core.Scenario.Given;

/// <summary>A customer account created for the scenario, which becomes its default customer.</summary>
public class GivenNewCustomer : BaseGiven, IGivenNewCustomer
{
    private string _firstName = DefaultFirstName;
    private string _lastName = DefaultLastName;
    private string? _email;
    private UserIdentity? _identity;

    public GivenNewCustomer(GivenStage givenClause) : base(givenClause)
    {
    }

    public GivenNewCustomer WithFirstName(string firstName)
    {
        _firstName = firstName;
        return this;
    }

    IGivenNewCustomer IGivenNewCustomer.WithFirstName(string firstName) => WithFirstName(firstName);

    public GivenNewCustomer WithLastName(string lastName)
    {
        _lastName = lastName;
        return this;
    }

    IGivenNewCustomer IGivenNewCustomer.WithLastName(string lastName) => WithLastName(lastName);

    public GivenNewCustomer WithEmail(string email)
    {
        _email = email;
        return this;
    }

    IGivenNewCustomer IGivenNewCustomer.WithEmail(string email) => WithEmail(email);

    /// <summary>Creates the account, once.</summary>
    internal override async Task Execute(UseCaseDsl app)
    {
        _identity ??= await app.NewCustomerAsync(_firstName, _lastName, _email);
    }

    /// <summary>The created customer; only valid once the scenario is being set up.</summary>
    internal UserIdentity Identity()
    {
        return _identity ?? throw new InvalidOperationException(
            "The new customer is created when the scenario is set up");
    }
}
