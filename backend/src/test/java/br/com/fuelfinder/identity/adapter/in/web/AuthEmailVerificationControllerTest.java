package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.application.exception.InvalidEmailVerificationTokenException;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailCommand;
import br.com.fuelfinder.identity.application.port.in.VerifyEmailUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthEmailVerificationController.class)
@Import({
        RegistrationHttpExceptionHandler.class,
        WebSecurityTestConfiguration.class
})
@ActiveProfiles("test")
class AuthEmailVerificationControllerTest {

    private static final String URL =
            "/api/v1/auth/verify-email";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VerifyEmailUseCase useCase;

    @Test
    void shouldVerifyEmailSuccessfully() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"token":"valid-verification-token"}
                        """))
                .andExpect(status().isNoContent());

        verify(useCase).execute(any(VerifyEmailCommand.class));
    }

    @Test
    void shouldRejectBlankToken() throws Exception {
        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"token":""}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectOversizedToken() throws Exception {
        String json = "{\"token\":\"" + "a".repeat(513) + "\"}";

        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        doThrow(new InvalidEmailVerificationTokenException())
                .when(useCase)
                .execute(any(VerifyEmailCommand.class));

        mvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"token":"invalid-verification-token"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
