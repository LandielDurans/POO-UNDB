package br.com.fuelfinder.identity.application.model;

public enum VerificationEmailOutboxProcessingResult {
    NO_EVENT,

    DISPATCH_REQUESTED,

    NO_ACTION_REQUIRED,

    RETRY_SCHEDULED,

    EXHAUSTED
}
