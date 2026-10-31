package br.com.fuelfinder.identity.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        @Schema(example = "maria@example.com")
        String email,

        @NotBlank
        @Size(max = 512)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
        String password

) {
    @Override
    public String toString() {
        return "LoginRequest[REDACTED]";
    }
}
