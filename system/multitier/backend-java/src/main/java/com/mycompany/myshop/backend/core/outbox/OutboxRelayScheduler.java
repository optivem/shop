package com.mycompany.myshop.backend.core.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox every {@code outbox.relay.poll-interval} (fixed delay, so runs never overlap within
 * one instance). Absent when {@code outbox.relay.enabled=false}: events are still written, nothing
 * delivers them.
 *
 * <p>In-process polling only runs while the process has CPU. On Cloud Run with request-based CPU and
 * min instances 0, events can wait until the next request wakes the instance: fine for an audit row,
 * not for anything with a deadline.
 */
@Component
@ConditionalOnProperty(prefix = "outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler implements SchedulingConfigurer {

    private final OutboxRelay relay;
    private final OutboxRelaySettings settings;

    public OutboxRelayScheduler(OutboxRelay relay, OutboxRelaySettings settings) {
        this.relay = relay;
        this.settings = settings;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(relay::relayBatch, settings.pollInterval());
    }
}
