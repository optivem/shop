package com.mycompany.myshop.backend.config;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceIdProblemDetailAdviceTest {

    @Test
    void stampsTheCurrentTraceIdOnAProblemDetail() {
        var tracer = mock(Tracer.class, RETURNS_DEEP_STUBS);
        when(tracer.currentSpan().context().traceId()).thenReturn("4bf92f3577b34da6a3ce929d0e0e4736");
        var advice = new TraceIdProblemDetailAdvice(provider(tracer));
        var problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        advice.beforeBodyWrite(problem, null, null, null, null, null);

        assertThat(problem.getProperties()).containsEntry("traceId", "4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void leavesTheBodyAloneWithoutACurrentSpan() {
        var tracer = mock(Tracer.class);
        var advice = new TraceIdProblemDetailAdvice(provider(tracer));
        var problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        advice.beforeBodyWrite(problem, null, null, null, null, null);

        assertThat(problem.getProperties()).isNull();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<Tracer> provider(Tracer tracer) {
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(tracer);
        return provider;
    }
}
