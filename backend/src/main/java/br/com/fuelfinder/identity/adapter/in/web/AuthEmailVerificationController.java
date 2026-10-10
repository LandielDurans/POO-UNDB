package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.adapter.in.web.dto.VerifyEmailRequest;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Authentication",
        description = "Local registration and account verification"
)
public class AuthEmailVerificationController {

    private final VerifyEmailUseCase verifyEmail;

    public AuthEmailVerificationController(
            VerifyEmailUseCase verifyEmail
    ) {
        this.verifyEmail = Objects.requireNonNull(verifyEmail);
    }

    @PostMapping(
            value = "/verify-email",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            operationId = "verifyEmail",
            summary = "Verify an email address",
            description = """
                    Consumes a valid email verification token
                    and activates the pending user account.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Email verified successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired token"
            )
    })
    public ResponseEntity<Void> verify(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        verifyEmail.execute(
                new VerifyEmailCommand(request.token())
        );

        return ResponseEntity.noContent().build();
    }
}
