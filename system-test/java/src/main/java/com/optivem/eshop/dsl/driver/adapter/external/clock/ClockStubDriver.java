package com.optivem.eshop.dsl.driver.adapter.external.clock;

import com.optivem.eshop.dsl.driver.port.external.clock.ClockDriver;

import com.optivem.eshop.dsl.driver.adapter.external.clock.client.ClockStubClient;
import com.optivem.eshop.dsl.driver.adapter.external.clock.client.dtos.ExtGetTimeResponse;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.GetTimeResponse;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.ReturnsTimeRequest;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Closer;
import com.optivem.eshop.dsl.common.Converter;
import com.optivem.eshop.dsl.common.Result;

public class ClockStubDriver implements ClockDriver {
    private final ClockStubClient client;

    public ClockStubDriver(String baseUrl) {
        this.client = new ClockStubClient(baseUrl);
    }

    @Override
    public void close() {
        Closer.close(client);
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
        var time = Converter.toInstant(request.getTime());

        var extResponse = ExtGetTimeResponse.builder()
                .time(time)
                .build();

        return client.configureGetTime(extResponse)
                .mapError(ext -> ErrorResponse.builder().message(ext.getMessage()).build());
    }
}
