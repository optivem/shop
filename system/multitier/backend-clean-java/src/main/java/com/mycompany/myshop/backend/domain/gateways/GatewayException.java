package com.mycompany.myshop.backend.domain.gateways;

// Beside the ports rather than beside the adapters, because a port declares both halves of its
// contract: what it answers with, and how it says it could not answer. While this type lived in
// infrastructure, no layer allowed to catch it was allowed to name it -- so "the ERP is down" could
// only ever surface as the catch-all 500, indistinguishable from a bug of ours.
//
// The subclasses name the system, not the failure mode: from the core's point of view an unreachable
// ERP, a 500 from the ERP, and a body the ERP sent that we could not read are one outcome -- we asked
// a system we do not control and did not get an answer.
public abstract class GatewayException extends RuntimeException {

    protected GatewayException(String message) {
        super(message);
    }

    protected GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
