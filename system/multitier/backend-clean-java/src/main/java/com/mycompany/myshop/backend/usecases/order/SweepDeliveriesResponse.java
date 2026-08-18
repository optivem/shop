package com.mycompany.myshop.backend.usecases.order;

import java.time.Instant;

public class SweepDeliveriesResponse {

    private Instant cutoff;
    private int deliveredCount;

    public Instant getCutoff() {
        return cutoff;
    }

    public void setCutoff(Instant cutoff) {
        this.cutoff = cutoff;
    }

    public int getDeliveredCount() {
        return deliveredCount;
    }

    public void setDeliveredCount(int deliveredCount) {
        this.deliveredCount = deliveredCount;
    }
}
