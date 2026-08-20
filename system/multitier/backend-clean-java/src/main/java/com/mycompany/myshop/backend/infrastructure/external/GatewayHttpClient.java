package com.mycompany.myshop.backend.infrastructure.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// The one place that knows how to call a system we do not control: the timeouts, the connection pool,
// and how many times a request is worth repeating. It was four copies of the same twelve lines before,
// which is how the two timeouts came to be written six times and the client rebuilt on every call.
//
// It deliberately does not interpret the response. Status meaning is the caller's, and it differs:
// a 404 from the ERP's product endpoint is "no such product", a 404 from its promotion endpoint is a
// broken deployment. The shared part is transport, not semantics -- which is also why this returns
// the response and rethrows IOException rather than wrapping anything: each adapter keeps building
// its own GatewayException with its own message, naming the SKU or the country it was asking about.
public final class GatewayHttpClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayHttpClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // Three attempts, not more: a retry is for the failure that is over before you finish reading
    // about it -- a dropped connection, a node being replaced mid-request. Anything still failing on
    // the third attempt is an outage, and hammering it is how a caller turns someone else's outage
    // into their own.
    private static final int MAX_ATTEMPTS = 3;
    private static final long FIRST_BACKOFF_MILLIS = 100;

    // Shared, and static because it is stateless and thread-safe: an HttpClient owns a connection pool
    // and an executor, so building one per call threw away every pooled connection and stood up a
    // thread pool in order to make a single request.
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private GatewayHttpClient() {
    }

    public static HttpResponse<String> get(String url) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        // Every call this class makes is a GET, so retrying cannot duplicate a side effect. If a POST
        // ever needs to go through here, this loop is not automatically safe for it.
        for (var attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (!isWorthRetrying(response.statusCode())) {
                    return response;
                }
                logRetry(url, attempt, String.valueOf(response.statusCode()));
            } catch (IOException e) {
                // Not swallowed: if the last attempt fails too, its own exception is what propagates,
                // carrying the same class and message this one would have.
                logRetry(url, attempt, e.getClass().getSimpleName());
            }
            Thread.sleep(backoffMillis(attempt));
        }

        // The final attempt is the answer, whatever it is. A 500 here is returned rather than retried
        // so the caller can build the message that names it, and an IOException here propagates into
        // the caller's own catch block.
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // 5xx is the upstream saying it failed, 429 is it saying "not so fast" -- both can differ on the
    // next attempt. A 4xx is an answer about our request, and repeating an identical request cannot
    // change it.
    private static boolean isWorthRetrying(int statusCode) {
        return statusCode >= 500 || statusCode == 429;
    }

    private static long backoffMillis(int attempt) {
        return FIRST_BACKOFF_MILLIS * attempt;
    }

    private static void logRetry(String url, int attempt, String reason) {
        if (log.isWarnEnabled()) {
            log.warn("Retrying {} after attempt {} of {} failed with {}",
                    url.replaceAll("[\r\n]", "_"), attempt, MAX_ATTEMPTS, reason);
        }
    }
}
