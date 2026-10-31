package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.adapter.in.web.dto.UserProfileResponse;
import br.com.fuelfinder.identity.application.port.in.GetUserProfileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Authenticated user's account")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final GetUserProfileUseCase getProfile;

    public UserProfileController(GetUserProfileUseCase getProfile) {
        this.getProfile = Objects.requireNonNull(getProfile);
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "getCurrentUser",
            summary = "Get the authenticated user's profile"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserProfileResponse> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return getProfile.execute(AuthenticatedUser.idOf(jwt))
                .map(UserProfileResponse::of)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .build());
    }
}
