package com.mycompany.myshop.backend.testkit.driver.port.external.clock;

public interface ClockDriver {

    void goToClock();

    void returnsTime(String isoInstant);

    void failsForTime(int status, String body);
}
