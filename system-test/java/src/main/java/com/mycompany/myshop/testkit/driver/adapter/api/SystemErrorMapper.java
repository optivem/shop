package com.mycompany.myshop.testkit.driver.adapter.api;

import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors.ProblemDetailResponse;

public class SystemErrorMapper {
    private SystemErrorMapper() {
    }

    public static SystemError from(ProblemDetailResponse problemDetail) {
        var message = problemDetail.getDetail() != null ? problemDetail.getDetail() : "Request failed";

        if (problemDetail.getErrors() != null && !problemDetail.getErrors().isEmpty()) {
            var fieldErrors = problemDetail.getErrors().stream()
                    .map(e -> new SystemError.FieldError(e.getField(), e.getMessage(), e.getCode()))
                    .toList();
            return withResponseMetadata(SystemError.builder().message(message).fields(fieldErrors), problemDetail);
        }

        return withResponseMetadata(SystemError.builder().message(message), problemDetail);
    }

    private static SystemError withResponseMetadata(SystemError.SystemErrorBuilder builder, ProblemDetailResponse problemDetail) {
        return builder
                .httpStatus(problemDetail.getHttpStatus())
                .retryAfter(problemDetail.getRetryAfter())
                .contentType(problemDetail.getContentType())
                .build();
    }
}
