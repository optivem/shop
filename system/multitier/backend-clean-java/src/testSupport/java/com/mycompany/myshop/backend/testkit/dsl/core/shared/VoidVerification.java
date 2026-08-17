package com.mycompany.myshop.backend.testkit.dsl.core.shared;

/** For use cases whose success carries no payload (e.g. {@code 204 No Content} on publish coupon). */
public class VoidVerification extends ResponseVerification<Void> {

    public VoidVerification(Void response) {
        super(response);
    }
}
