package com.mycompany.myshop.backend.infrastructure.external.clock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

/**
 * The clock system's wire shape. Lives in the adapter, never crosses into the domain.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTimeResponse {
    private Instant time;
}
