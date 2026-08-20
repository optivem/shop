package com.mycompany.myshop.backend.presentation;

import com.mycompany.myshop.backend.usecases.queries.OrderCursor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

// Turns a page cursor into an opaque token and back.
//
// Opaque on purpose. A cursor is the sort key of the last row on a page -- for orders, a
// timestamp and an order number -- and a client that can read a sort key will eventually build one
// by hand, at which point the sort key can never change again without breaking that client. Base64
// does not make the cursor secret; it makes it obviously not the client's to construct.
//
// Encoding lives in presentation because it is a wire format, and the wire is this layer's
// business. The use case hands out and receives the typed cursor and cannot tell which encoding was
// chosen here.
@Component
public class CursorCodec {

    // The timestamp is written in full ISO-8601 rather than as epoch millis: order_timestamp is a
    // TIMESTAMPTZ with microsecond precision, and a cursor rounded to the millisecond would sit
    // between two real rows -- skipping one or repeating one at the page boundary.
    private static final String SEPARATOR = "|";

    public String encodeOrder(OrderCursor cursor) {
        if (cursor == null) {
            return null;
        }
        return encode(cursor.orderTimestamp() + SEPARATOR + cursor.orderNumber());
    }

    public OrderCursor decodeOrder(String token) {
        if (isBlank(token)) {
            return null;
        }

        var decoded = decode(token);
        var separator = decoded.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new InvalidCursorException();
        }

        try {
            // indexOf, not split: everything after the first separator is the order number, so an
            // order number that itself contains one survives the round trip.
            return new OrderCursor(
                    Instant.parse(decoded.substring(0, separator)),
                    decoded.substring(separator + SEPARATOR.length()));
        } catch (DateTimeParseException e) {
            throw new InvalidCursorException();
        }
    }

    public String encodeCoupon(String code) {
        return code == null ? null : encode(code);
    }

    public String decodeCoupon(String token) {
        return isBlank(token) ? null : decode(token);
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
