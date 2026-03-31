package com.optivem.eshop.dsl.driver.adapter.shop.api.client.dtos.errors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailResponse {
    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
    private String timestamp;
    private List<FieldError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private String code;
        private Object rejectedValue;
    }
}

