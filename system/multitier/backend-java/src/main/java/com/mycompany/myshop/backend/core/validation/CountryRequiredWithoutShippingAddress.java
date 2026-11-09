package com.mycompany.myshop.backend.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A place-order request needs a top-level country unless it carries a shipping address, whose
 * country is then the tax country. The violation is reported on the {@code country} field.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CountryRequiredWithoutShippingAddressValidator.class)
public @interface CountryRequiredWithoutShippingAddress {
    String message() default "Country must not be empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
