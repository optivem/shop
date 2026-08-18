package com.mycompany.myshop.backend.usecases;

/**
 * The one shape every use case has: one request in, one declared outcome out. This is Uncle Bob's
 * Interactor from <em>Clean Architecture</em>, with the return wrapped in a {@link Result}.
 *
 * <p>Splitting a service into one class per use case fixes dependencies and naming. It says nothing
 * about what those classes look like from outside, which is how a codebase ends up with four input
 * shapes and three output shapes across seven use cases. This interface is what says it.
 *
 * <p>Interface only — deliberately no base class. Inherited behaviour would live in a class that
 * appears in none of the use case signatures, which is the first step toward the base-class sprawl
 * that gives interactor-style patterns their bad reputation.
 *
 * <p>There is no {@code CommandBus} here either, and at this fleet size there should not be: seven
 * use cases wired by constructor in {@code UseCaseConfig} is the right amount of infrastructure. A
 * bus earns its keep once controllers grow a dependency per use case, or once a cross-cutting
 * concern — logging, transactions, authorization — has to apply uniformly. This uniform signature
 * is precisely what makes that bus possible later without touching a use case.
 *
 * @param <TRequest> the single input object
 * @param <TResponse> what success produces, or {@link Void} for a use case that produces nothing
 */
public interface UseCase<TRequest, TResponse> {

    Result<TResponse, UseCaseError> execute(TRequest request);
}
