package com.mycompany.myshop.backend.config;

import com.mycompany.myshop.backend.core.outbox.OutboxRelaySettings;
import com.mycompany.myshop.backend.core.outbox.OutboxStore;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Transactional outbox (backend-java only, a worked example): binds outbox.relay.* into
// OutboxRelaySettings, turns on scheduling for OutboxRelayScheduler, and publishes the backlog as the
// outbox.pending gauge (outbox_pending in Prometheus/Grafana under the otel-lgtm profile). The gauge
// is read when metrics are published, so it keeps counting while the relay is switched off.
@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxRelaySettings.class)
public class OutboxConfig {

    public static final String PENDING_GAUGE = "outbox.pending";

    @Bean
    public MeterBinder outboxPendingGauge(OutboxStore store) {
        return registry -> Gauge.builder(PENDING_GAUGE, store, OutboxStore::countPending)
                .description("Outbox events not yet delivered, including those that ran out of attempts")
                .register(registry);
    }
}
