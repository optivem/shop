package com.optivem.eshop.dsl.driver.adapter.external.clock;

import com.optivem.eshop.dsl.driver.port.external.clock.ClockDriver;

import com.optivem.eshop.dsl.driver.adapter.external.clock.client.ClockRealClient;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.GetTimeResponse;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.ReturnsTimeRequest;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

public class ClockRealDriver implements ClockDriver {
    private final ClockRealClient client;

    public ClockRealDriver() {
        this.client = new ClockRealClient();
    }

    @Override
    public void close() {
        // No resources to close - ClockRealClient has no HTTP or other resources
    }

    @Override
    public Result<Void, ErrorResponse> goToClock() {
        return client.checkHealth()
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<GetTimeResponse, ErrorResponse> getTime() {
        return client.getTime()
                .map(ext -> GetTimeResponse.builder().time(ext.getTime()).build())
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }

    @Override
    public Result<Void, ErrorResponse> returnsTime(ReturnsTimeRequest request) {
        // No-op because real clock cannot be configured
        return Result.success();
    }
}
