package com.mycompany.myshop.backend.usecases.order;

// A null size means "use the default"; anything else must be 1..MAX_SIZE. A null page
// means the first one, which is page 1 rather than page 0 -- see PageSpec for why.
public record BrowseOrderHistoryRequest(String orderNumberFilter, Integer page, Integer size) { }
