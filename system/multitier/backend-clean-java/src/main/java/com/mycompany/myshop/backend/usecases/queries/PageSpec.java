package com.mycompany.myshop.backend.usecases.queries;

// How much of a list to read, and where to resume reading it.
//
// Deliberately ours rather than Spring's Pageable. A port that names a framework type has
// imported the framework into the layer that was supposed to be free of it -- and Pageable
// brings OFFSET with it, which is the mechanism this vocabulary exists to avoid. This record
// knows no SQL; the adapter decides how to honour it.
//
// Not in the domain either, for the reason the rest of this package exists: nothing on the
// command side pages. Paging is a read-side concern, so it lives with the read side.
//
// Not PageSpecRequest, for two reasons that agree. In this codebase a *Request is
// one use case's wire contract -- REQUESTS_AND_RESPONSES_LIVE_WITH_THEIR_USECASE makes that
// executable -- and this type belongs to no use case, it is shared by every paged port. And Spring
// Data's own paging type is called PageRequest, which is the last name to give the class
// whose whole purpose is not being it.
//
// cursor is null for the first page. Its type is the caller's, because what
// identifies "where I got to" differs per list: an order needs a timestamp and a tiebreaker
// (OrderCursor), a coupon needs only its code.
public record PageSpec<C>(int size, C cursor) {

    public static final int DEFAULT_SIZE = 50;

    // The ceiling exists because "how many rows may one request cost?" is a question the caller is
    // not allowed to answer. Without it, ?size=1000000 is the unbounded read all over again,
    // wearing a page's clothes.
    public static final int MAX_SIZE = 200;

    public static boolean isValidSize(Integer requestedSize) {
        return requestedSize == null || (requestedSize >= 1 && requestedSize <= MAX_SIZE);
    }

    public static int sizeOrDefault(Integer requestedSize) {
        return requestedSize == null ? DEFAULT_SIZE : requestedSize;
    }
}
