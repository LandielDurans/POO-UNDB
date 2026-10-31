package br.com.fuelfinder.identity.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(

        @NotBlank
        @Size(max = 512)
        @Schema(
                description = "Refresh token received at login or last refresh",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String refreshToken

) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[REDACTED]";
    }
}
