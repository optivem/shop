package com.mycompany.myshop.backend.usecases.queries;

import java.util.List;

// One page of a list, and how many rows there are in total.
//
// The total is here because a numbered-page client cannot be built without it: "page 3 of 26" and a
// row of page buttons are both arithmetic on totalElements, and neither can be derived from the
// rows on this page. It costs a second statement -- a COUNT(*) over the same predicate as the
// SELECT -- which is the price of the page numbers.
//
// Both the page number and the size are carried back rather than left for the caller to remember,
// so that a response can be built from this record alone. They are the ones that were honoured,
// which is not always the ones that were asked for: a missing size becomes DEFAULT_SIZE
// somewhere above, and echoing the effective values is what lets a client tell which it got.
public record Page<T>(List<T> items, int page, int size, long totalElements) {

    // Ceiling division. An empty list is zero pages rather than one: a client that renders "page 1
    // of 0" is showing a page that does not exist, and "no results" is the honest empty state.
    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
