package br.com.fuelfinder.identity.adapter.out.persistence.jpa.entity;

public enum VerificationEmailOutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    EXHAUSTED
}
