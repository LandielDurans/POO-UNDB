package br.com.fuelfinder.identity.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(

        @NotBlank
        @Size(max = 512)
        @Schema(
                description = "Raw email verification token",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String token

) {
    @Override
    public String toString() {
        return "VerifyEmailRequest[REDACTED]";
    }
}
