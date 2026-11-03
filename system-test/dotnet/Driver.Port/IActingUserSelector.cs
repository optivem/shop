namespace Driver.Port;

/// <summary>
/// Selects who subsequent driver operations are performed as. Kept apart from <see cref="IMyShopDriver"/>, which
/// declares only system operations (each taking a *Request and returning Result&lt;*Response, …&gt;): choosing the
/// acting identity is session state, not an operation on the system.
/// </summary>
public interface IActingUserSelector
{
    /// <summary>Selects who subsequent operations are performed as.</summary>
    void ActAs(UserIdentity identity);
}
