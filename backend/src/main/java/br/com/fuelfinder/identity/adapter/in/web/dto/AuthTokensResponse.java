package br.com.fuelfinder.identity.adapter.in.web.dto;

import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.time.Instant;

@Schema(description = "Access and refresh tokens for an authenticated session")
public record AuthTokensResponse(

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Short-lived JWT sent as 'Authorization: Bearer <token>'")
        String accessToken,

        @Schema(description = "Seconds until the access token expires", example = "900")
        long expiresIn,

        Instant accessTokenExpiresAt,

        @Schema(description = "Opaque token used once to obtain a new token pair")
        String refreshToken,

        Instant refreshTokenExpiresAt

) {
    public static AuthTokensResponse of(AuthenticatedSession session) {
        IssuedAccessToken accessToken = session.accessToken();

        return new AuthTokensResponse(
                "Bearer",
                accessToken.value(),
                Duration.between(
                        accessToken.issuedAt(),
                        accessToken.expiresAt()
                ).toSeconds(),
                accessToken.expiresAt(),
                session.refreshToken(),
                session.refreshTokenExpiresAt()
        );
    }

    @Override
    public String toString() {
        return "AuthTokensResponse[REDACTED]";
    }
}
