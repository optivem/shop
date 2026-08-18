package com.mycompany.myshop.backend.domain.gateways;

import java.time.Instant;

public interface ClockGateway {

    Instant getCurrentTime();
}
