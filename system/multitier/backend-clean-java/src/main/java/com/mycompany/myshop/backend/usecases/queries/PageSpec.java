package com.mycompany.myshop.backend.usecases.queries;

// How much of a list to read, and which page of it.
//
// Deliberately ours rather than Spring's Pageable. A port that names a framework type has
// imported the framework into the layer that was supposed to be free of it, and the point of a port
// is that the adapter behind it could be JDBC, a file, or a stub in a test. This record knows no
// SQL; the adapter decides how to honour it.
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
// Pages are numbered from one, not from zero. Spring Data numbers from zero and every team using it
// eventually ships the off-by-one where page 1 of the UI asks for page 1 of the API and silently
// skips the first rows. The wire says page=1 for the first page because that is what the
// person reading the URL already believes it says.
public record PageSpec(int page, int size) {

    public static final int FIRST_PAGE = 1;

    public static final int DEFAULT_SIZE = 50;

    // The ceiling exists because "how many rows may one request cost?" is a question the caller is
    // not allowed to answer. Without it, ?size=1000000 is the unbounded read all over again,
    // wearing a page's clothes.
    public static final int MAX_SIZE = 200;

    public static boolean isValidPage(Integer requestedPage) {
        return requestedPage == null || requestedPage >= FIRST_PAGE;
    }

    public static boolean isValidSize(Integer requestedSize) {
        return requestedSize == null || (requestedSize >= 1 && requestedSize <= MAX_SIZE);
    }

    public static int pageOrFirst(Integer requestedPage) {
        return requestedPage == null ? FIRST_PAGE : requestedPage;
    }

    public static int sizeOrDefault(Integer requestedSize) {
        return requestedSize == null ? DEFAULT_SIZE : requestedSize;
    }

    // How many rows the database is asked to skip. Long rather than int because the product of two
    // ints overflows, and a page number is a number a stranger chose: page=2000000000 must come
    // back empty, not wrap around to a negative offset and read from somewhere unexpected.
    public long offset() {
        return (long) (page - FIRST_PAGE) * size;
    }
}
