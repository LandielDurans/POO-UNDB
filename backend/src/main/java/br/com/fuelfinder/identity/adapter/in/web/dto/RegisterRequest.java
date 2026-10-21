package br.com.fuelfinder.identity.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Local user registration request")
public record RegisterRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        @Schema(example = "Maria Silva")
        String name,

        @NotBlank
        @Email
        @Size(max = 254)
        @Schema(example = "maria@example.com")
        String email,

        @NotBlank
        @Schema(
                description = "Password with 15 to 128 Unicode code points",
                minLength = 15,
                maxLength = 128,
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String password

) {
    @Override
    public String toString() {
        return "RegisterRequest[REDACTED]";
    }
}
