package com.mycompany.myshop.backend.presentation;

// A cursor that did not come from CursorCodec. It is a 400 rather than a 422: the token is
// not a field the caller got wrong, it is a value the caller was never meant to author.
//
// Carries no detail about why it failed, deliberately. Explaining the decode is explaining the
// format, which is the thing the token is opaque in order not to say.
public class InvalidCursorException extends RuntimeException {

    private static final String MESSAGE = "The cursor is not valid. Use the nextCursor returned by a previous page.";

    public InvalidCursorException() {
        super(MESSAGE);
    }
}
