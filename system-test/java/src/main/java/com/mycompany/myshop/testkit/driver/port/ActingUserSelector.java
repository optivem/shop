package com.mycompany.myshop.testkit.driver.port;

/**
 * Selects who subsequent driver operations are performed as. Kept apart from {@link MyShopDriver}, which
 * declares only system operations (each taking a *Request and returning Result&lt;*Response, …&gt;): choosing the
 * acting identity is session state, not an operation on the system.
 */
public interface ActingUserSelector {
    /** Selects who subsequent operations are performed as. */
    void actAs(UserIdentity identity);
}
