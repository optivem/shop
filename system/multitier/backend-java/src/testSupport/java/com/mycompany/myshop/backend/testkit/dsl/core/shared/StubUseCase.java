package com.mycompany.myshop.backend.support.core.shared;

/**
 * A use case against an external-system stub: programming a WireMock mapping in the test's own
 * process, which cannot fail — hence no result to assert on.
 */
public interface StubUseCase {
    void execute();
}
