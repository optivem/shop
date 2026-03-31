package com.optivem.eshop.dsl.driver.adapter.shop.api;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.driver.adapter.shop.api.client.dtos.errors.ProblemDetailResponse;

public class SystemErrorMapper {
    private SystemErrorMapper() {
    }

    public static ErrorResponse from(ProblemDetailResponse problemDetail) {
        var message = problemDetail.getDetail() != null ? problemDetail.getDetail() : "Request failed";

        if (problemDetail.getErrors() != null && !problemDetail.getErrors().isEmpty()) {
            var fieldErrors = problemDetail.getErrors().stream()
                    .map(e -> new ErrorResponse.FieldError(e.getField(), e.getMessage(), e.getCode()))
                    .toList();
            return ErrorResponse.of(message, fieldErrors);
        }

        return ErrorResponse.of(message);
    }
}

