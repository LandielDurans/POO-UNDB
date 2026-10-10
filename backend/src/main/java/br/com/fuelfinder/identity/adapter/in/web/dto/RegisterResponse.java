package br.com.fuelfinder.identity.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Registration accepted for verification")
public record RegisterResponse(

        UUID userId,

        @Schema(
                allowableValues = "PENDING_VERIFICATION",
                example = "PENDING_VERIFICATION"
        )
        String status

) {
}
