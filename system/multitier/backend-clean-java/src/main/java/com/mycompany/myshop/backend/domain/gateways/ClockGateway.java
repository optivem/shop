package com.mycompany.myshop.backend.domain.gateways;

import java.time.Instant;

/**
 * The domain's port to whatever tells it the current time. Implemented from
 * {@code infrastructure.external.clock}.
 */
public interface ClockGateway {

    Instant getCurrentTime();
}
