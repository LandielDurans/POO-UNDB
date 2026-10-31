package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.application.model.AuthenticatedSession;
import br.com.fuelfinder.identity.application.model.IssuedAccessToken;
import br.com.fuelfinder.identity.application.port.in.LoginCommand;
import br.com.fuelfinder.identity.application.port.in.LoginUseCase;
import br.com.fuelfinder.identity.application.port.in.LogoutAllSessionsUseCase;
import br.com.fuelfinder.identity.application.port.in.LogoutUseCase;
import br.com.fuelfinder.identity.application.port.in.RefreshSessionUseCase;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthSessionController.class)
@Import({
        RegistrationHttpExceptionHandler.class,
        AuthenticationHttpExceptionHandler.class,
        WebSecurityTestConfiguration.class
})
@ActiveProfiles("test")
class AuthSessionControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private LoginUseCase login;

    @MockitoBean
    private RefreshSessionUseCase refresh;

    @MockitoBean
    private LogoutUseCase logout;

    @MockitoBean
    private LogoutAllSessionsUseCase logoutAll;

    @Test
    void shouldReturnTokenPairOnLogin() throws Exception {
        when(login.execute(any(LoginCommand.class))).thenReturn(session());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "Maria@Example.com", "password": "Senha2026"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void shouldValidateLoginPayload() throws Exception {
        for (String body : new String[]{
                """
                {"email": "invalido", "password": "Senha2026"}
                """,
                """
                {"email": "maria@example.com", "password": ""}
                """,
                """
                {"email": "maria@example.com"}
                """
        }) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(login);
    }

    @Test
    void shouldRequireRefreshToken() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(refresh);
    }

    @Test
    void shouldLogoutAllSessionsOfAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();

        mvc.perform(post("/api/v1/auth/logout-all")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        verify(logoutAll).execute(UserId.from(userId));
    }

    @Test
    void shouldRequireAuthenticationForLogoutAll() throws Exception {
        mvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verifyNoInteractions(logoutAll);
    }

    private static AuthenticatedSession session() {
        return new AuthenticatedSession(
                new IssuedAccessToken("access-jwt", NOW, NOW.plusSeconds(900)),
                "refresh-token",
                NOW.plusSeconds(7 * 24 * 3600)
        );
    }
}
