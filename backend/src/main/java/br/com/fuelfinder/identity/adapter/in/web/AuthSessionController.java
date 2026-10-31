package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.adapter.in.web.dto.AuthTokensResponse;
import br.com.fuelfinder.identity.adapter.in.web.dto.LoginRequest;
import br.com.fuelfinder.identity.adapter.in.web.dto.RefreshTokenRequest;
import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.port.in.LoginCommand;
import br.com.fuelfinder.identity.application.port.in.LoginUseCase;
import br.com.fuelfinder.identity.application.port.in.LogoutAllSessionsUseCase;
import br.com.fuelfinder.identity.application.port.in.LogoutUseCase;
import br.com.fuelfinder.identity.application.port.in.RefreshSessionUseCase;
import br.com.fuelfinder.identity.domain.valueobject.Email;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Authentication",
        description = "Local registration, account verification and sessions"
)
public class AuthSessionController {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refresh;
    private final LogoutUseCase logout;
    private final LogoutAllSessionsUseCase logoutAll;

    public AuthSessionController(
            LoginUseCase login,
            RefreshSessionUseCase refresh,
            LogoutUseCase logout,
            LogoutAllSessionsUseCase logoutAll
    ) {
        this.login = Objects.requireNonNull(login);
        this.refresh = Objects.requireNonNull(refresh);
        this.logout = Objects.requireNonNull(logout);
        this.logoutAll = Objects.requireNonNull(logoutAll);
    }

    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "login",
            summary = "Log in with email and password",
            description = """
                    Opens a new session. Unknown email and wrong
                    password return the same 401 response.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session opened"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials (code INVALID_CREDENTIALS)"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Email not verified (EMAIL_NOT_VERIFIED) "
                            + "or account blocked (ACCOUNT_BLOCKED)"
            )
    })
    public ResponseEntity<AuthTokensResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthenticatedSession session = login.execute(
                new LoginCommand(Email.of(request.email()), request.password())
        );

        return ResponseEntity.ok(AuthTokensResponse.of(session));
    }

    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "refreshSession",
            summary = "Exchange a refresh token for a new token pair",
            description = """
                    The refresh token is single use: the response
                    carries a new one and the old one stops working.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renewed"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid, used, revoked or expired refresh "
                            + "token (code INVALID_REFRESH_TOKEN)"
            )
    })
    public ResponseEntity<AuthTokensResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthenticatedSession session = refresh.execute(request.refreshToken());

        return ResponseEntity.ok(AuthTokensResponse.of(session));
    }

    @PostMapping(
            value = "/logout",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "logout",
            summary = "End the session of a refresh token",
            description = "Idempotent: unknown tokens also return 204."
    )
    @ApiResponse(responseCode = "204", description = "Session ended")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        logout.execute(request.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            operationId = "logoutAll",
            summary = "End every session of the authenticated user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessions ended"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal Jwt jwt
    ) {
        logoutAll.execute(AuthenticatedUser.idOf(jwt));

        return ResponseEntity.noContent().build();
    }
}
