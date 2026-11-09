package com.mycompany.myshop.core.validation;

import com.mycompany.myshop.core.dtos.PlaceOrderRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CountryRequiredWithoutShippingAddressValidator
        implements ConstraintValidator<CountryRequiredWithoutShippingAddress, PlaceOrderRequest> {

    @Override
    public boolean isValid(PlaceOrderRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getShippingAddress() != null) {
            return true;
        }
        var country = request.getCountry();
        if (country != null && !country.isBlank()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("country")
                .addConstraintViolation();
        return false;
    }
}
