package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.adapter.in.web.dto.RegisterRequest;
import br.com.fuelfinder.identity.adapter.in.web.dto.RegisterResponse;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.domain.valueobject.Email;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Authentication",
        description = "Local registration and account verification"
)
public class AuthRegistrationController {

    private final RegisterLocalUserUseCase registerLocalUser;

    public AuthRegistrationController(
            RegisterLocalUserUseCase registerLocalUser
    ) {
        this.registerLocalUser = Objects.requireNonNull(
                registerLocalUser
        );
    }

    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Register a local user",
            operationId = "registerLocalUser",
            description = """
                    Creates a pending account and queues an email
                    verification event. Sending the email is asynchronous.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "Registration accepted",
                    content = @Content(
                            schema = @Schema(
                                    implementation = RegisterResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration request",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Registration conflict",
                    content = @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(
                                    implementation = ProblemDetail.class
                            )
                    )
            )
    })
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterLocalUserCommand command =
                new RegisterLocalUserCommand(
                        request.name(),
                        Email.of(request.email()),
                        request.password()
                );

        UserId userId = registerLocalUser.execute(command);

        return ResponseEntity.accepted().body(
                new RegisterResponse(
                        userId.value(),
                        "PENDING_VERIFICATION"
                )
        );
    }
}
