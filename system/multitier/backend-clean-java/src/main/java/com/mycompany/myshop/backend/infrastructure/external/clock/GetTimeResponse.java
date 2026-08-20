package com.mycompany.myshop.backend.infrastructure.external.clock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class GetTimeResponse {
    private Instant time;
}
