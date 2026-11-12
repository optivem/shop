using Driver.Port;

namespace Dsl.Core.Scenario.Given;

/// <summary>
/// Scenario-local mapping from customer aliases to test customers.
/// <para>
/// The default customer (the no-argument forms) is always customer1. When a scenario uses the default customer,
/// customer1 is reserved for it and aliases are assigned from the remaining customers; otherwise aliases are
/// assigned from customer1 onwards. A scenario with a new customer has that one as its default customer, so its
/// aliases are assigned from customer1 onwards too. Within the pool, aliases are assigned in order of first use, and the same alias
/// always resolves to the same customer.
/// </para>
/// </summary>
internal class CustomerAliases
{
    private readonly List<string> _aliases = [];
    private bool _defaultCustomerUsed;
    private bool _newDefaultCustomer;

    public void Register(string alias)
    {
        if (string.IsNullOrWhiteSpace(alias))
        {
            throw new ArgumentException("Customer alias must not be blank", nameof(alias));
        }

        if (!_aliases.Contains(alias))
        {
            _aliases.Add(alias);
        }
    }

    public void ReserveDefaultCustomer()
    {
        _defaultCustomerUsed = true;
    }

    /// <summary>The default customer is a new one, not customer1, so customer1 is free for aliases.</summary>
    public void UseNewDefaultCustomer()
    {
        _newDefaultCustomer = true;
    }

    public static UserIdentity DefaultCustomer() => UserIdentity.Customer(0);

    public UserIdentity Resolve(string alias)
    {
        var index = _aliases.IndexOf(alias);
        if (index < 0)
        {
            throw new InvalidOperationException($"Customer alias '{alias}' was never declared in this scenario");
        }

        var offset = _defaultCustomerUsed && !_newDefaultCustomer ? 1 : 0;
        var customerIndex = index + offset;
        if (customerIndex >= UserIdentity.CustomerCount)
        {
            throw new InvalidOperationException(
                $"Cannot resolve customer alias '{alias}': the scenario needs "
                + $"{_aliases.Count + offset} distinct shared customers but only "
                + $"{UserIdentity.CustomerCount} test customers exist");
        }

        return UserIdentity.Customer(customerIndex);
    }
}
