package com.mycompany.myshop.testkit.driver.adapter.api.client.dtos.errors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.ResponseMetadataAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailResponse implements ResponseMetadataAware {
    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
    private String timestamp;
    private List<FieldError> errors;

    // From the HTTP response, not the body.
    @JsonIgnore
    private Integer httpStatus;
    @JsonIgnore
    private String retryAfter;
    @JsonIgnore
    private String contentType;

    @Override
    public void captureResponseMetadata(int httpStatus, String retryAfter, String contentType) {
        this.httpStatus = httpStatus;
        this.retryAfter = retryAfter;
        this.contentType = contentType;
    }

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

