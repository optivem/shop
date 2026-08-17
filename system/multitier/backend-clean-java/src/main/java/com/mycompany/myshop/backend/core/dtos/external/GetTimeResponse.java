package com.mycompany.myshop.backend.core.dtos.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetTimeResponse {
    private Instant time;
}
