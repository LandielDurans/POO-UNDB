package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.application.exception.EmailAlreadyRegisteredException;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthRegistrationController.class)
@Import({
        RegistrationHttpExceptionHandler.class,
        WebSecurityTestConfiguration.class
})
@ActiveProfiles("test")
class AuthRegistrationControllerTest {

    private static final String URL = "/api/v1/auth/register";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegisterLocalUserUseCase useCase;

    @Test
    void shouldAcceptValidRegistration() throws Exception {
        UUID id = UUID.randomUUID();

        when(useCase.execute(any(RegisterLocalUserCommand.class)))
                .thenReturn(UserId.from(id));

        mvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson())
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        jsonPath("$.userId")
                                .value(id.toString())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING_VERIFICATION")
                );

        verify(useCase).execute(
                any(RegisterLocalUserCommand.class)
        );
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        String json = """
                {
                  "name": "Maria Silva",
                  "email": "email-invalido",
                  "password": "MinhaSenhaSegura2026!"
                }
                """;

        mvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldRejectBlankPassword() throws Exception {
        String json = """
                {
                  "name": "Maria Silva",
                  "email": "maria@example.com",
                  "password": ""
                }
                """;

        mvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldReturnConflictForRegisteredEmail() throws Exception {
        when(useCase.execute(any(RegisterLocalUserCommand.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson())
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status").value(409)
                );
    }

    @Test
    void shouldReturnConflictForDatabaseUniqueViolation()
            throws Exception {

        SQLException sqlException =
                new SQLException(
                        "Duplicate value",
                        "23505"
                );

        when(useCase.execute(any(RegisterLocalUserCommand.class)))
                .thenThrow(
                        new DataIntegrityViolationException(
                                "Constraint conflict",
                                sqlException
                        )
                );

        mvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validJson())
                )
                .andExpect(status().isConflict());
    }

    private String validJson() {
        return """
                {
                  "name": "Maria Silva",
                  "email": "maria@example.com",
                  "password": "MinhaSenhaSegura2026!"
                }
                """;
    }
}
