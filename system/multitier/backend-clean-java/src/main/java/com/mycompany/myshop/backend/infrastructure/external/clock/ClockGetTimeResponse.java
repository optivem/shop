package com.mycompany.myshop.backend.infrastructure.external.clock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ClockGetTimeResponse {
    private Instant time;
}
