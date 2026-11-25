package com.mycompany.myshop.testkit.driver.port;

import java.util.function.Supplier;

/**
 * Selects who subsequent driver operations are performed as. Kept apart from {@link MyShopDriver}, which
 * declares only system operations (each taking a *Request and returning Result&lt;*Response, …&gt;): choosing the
 * acting identity is session state, not an operation on the system.
 */
public interface ActingUserSelector {
    /** Selects who subsequent operations are performed as. */
    void actAs(UserIdentity identity);

    /**
     * Who admin-only operations run as while no identity is selected: the scenario's own admin. Asked only when such
     * an operation runs, so a scenario without one creates no admin.
     */
    void useScenarioAdmin(Supplier<UserIdentity> admin);
}
