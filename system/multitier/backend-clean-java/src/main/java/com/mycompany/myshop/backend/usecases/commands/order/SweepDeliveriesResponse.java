package com.mycompany.myshop.backend.usecases.commands.order;

import java.time.Instant;

public record SweepDeliveriesResponse(Instant cutoff, int deliveredCount) { }
