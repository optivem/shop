package com.mycompany.myshop.backend.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Adds the current trace id ({@value #PROP_TRACE_ID}) to every ProblemDetail body, so an error a user
 * reports can be found in the logs, where the same id is logged as {@code trace_id}. Covers controller
 * and exception-handler responses; the security handlers call {@link #stamp} themselves.
 */
@ControllerAdvice
public class TraceIdProblemDetailAdvice implements ResponseBodyAdvice<Object> {

    public static final String PROP_TRACE_ID = "traceId";

    private final ObjectProvider<Tracer> tracer;

    public TraceIdProblemDetailAdvice(ObjectProvider<Tracer> tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof ProblemDetail problemDetail) {
            stamp(problemDetail);
        }
        return body;
    }

    public void stamp(ProblemDetail problemDetail) {
        Tracer current = tracer.getIfAvailable();
        Span span = current == null ? null : current.currentSpan();
        if (span != null) {
            problemDetail.setProperty(PROP_TRACE_ID, span.context().traceId());
        }
    }
}
