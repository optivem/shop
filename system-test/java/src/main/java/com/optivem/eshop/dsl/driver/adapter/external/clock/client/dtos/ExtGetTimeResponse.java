package com.optivem.eshop.dsl.driver.adapter.external.clock.client.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtGetTimeResponse {
    Instant time;
}
