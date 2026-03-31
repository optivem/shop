package com.optivem.eshop.dsl.core.usecase.external.clock.usecases;

import com.optivem.eshop.dsl.driver.port.external.clock.dtos.GetTimeResponse;
import com.optivem.eshop.dsl.core.shared.ResponseVerification;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.common.Converter;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class GetTimeVerification extends ResponseVerification<GetTimeResponse> {
    public GetTimeVerification(GetTimeResponse response, UseCaseContext context) {
        super(response, context);
    }

    public GetTimeVerification timeIsNotNull() {
        assertThat(getResponse().getTime())
                .withFailMessage("Expected time to be not null")
                .isNotNull();
        return this;
    }

    public GetTimeVerification time(Instant expectedTime) {
        assertThat(getResponse().getTime())
                .withFailMessage("Expected time %s to be equal to %s", getResponse().getTime(), expectedTime)
                .isEqualTo(expectedTime);
        return this;
    }

    public GetTimeVerification time(String expectedTime) {
        return time(Converter.toInstant(expectedTime));
    }

    public GetTimeVerification timeIsAfter(Instant time) {
        assertThat(getResponse().getTime())
                .withFailMessage("Expected time %s to be after %s", getResponse().getTime(), time)
                .isAfter(time);
        return this;
    }

    public GetTimeVerification timeIsBefore(Instant time) {
        assertThat(getResponse().getTime())
                .withFailMessage("Expected time %s to be before %s", getResponse().getTime(), time)
                .isBefore(time);
        return this;
    }

    public GetTimeVerification timeIsBetween(Instant start, Instant end) {
        assertThat(getResponse().getTime())
                .withFailMessage("Expected time %s to be between %s and %s", getResponse().getTime(), start, end)
                .isBetween(start, end);
        return this;
    }
}
