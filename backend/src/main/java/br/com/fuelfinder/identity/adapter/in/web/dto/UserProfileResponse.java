package br.com.fuelfinder.identity.adapter.in.web.dto;

import br.com.fuelfinder.identity.application.model.UserProfile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Authenticated user's profile")
public record UserProfileResponse(

        UUID id,

        @Schema(example = "Maria Silva")
        String name,

        @Schema(example = "maria@example.com")
        String email,

        @Schema(example = "[\"USER\"]")
        List<String> roles,

        @Schema(example = "ACTIVE")
        String status

) {
    public static UserProfileResponse of(UserProfile profile) {
        return new UserProfileResponse(
                profile.id().value(),
                profile.name(),
                profile.email().value(),
                profile.roles().stream().map(Enum::name).sorted().toList(),
                profile.status().name()
        );
    }
}
