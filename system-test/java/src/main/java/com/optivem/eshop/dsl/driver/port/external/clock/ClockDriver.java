package com.optivem.eshop.dsl.driver.port.external.clock;

import com.optivem.eshop.dsl.driver.port.external.clock.dtos.GetTimeResponse;
import com.optivem.eshop.dsl.driver.port.external.clock.dtos.ReturnsTimeRequest;
import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;
import com.optivem.eshop.dsl.common.Result;

public interface ClockDriver extends AutoCloseable {
    Result<Void, ErrorResponse> goToClock();
    Result<GetTimeResponse, ErrorResponse> getTime();
    Result<Void, ErrorResponse> returnsTime(ReturnsTimeRequest request);
}
