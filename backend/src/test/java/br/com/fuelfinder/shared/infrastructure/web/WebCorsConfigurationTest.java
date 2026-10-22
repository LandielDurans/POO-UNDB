package br.com.fuelfinder.shared.infrastructure.web;

import br.com.fuelfinder.identity.adapter.in.web.AuthRegistrationController;
import br.com.fuelfinder.identity.adapter.in.web.RegistrationHttpExceptionHandler;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserCommand;
import br.com.fuelfinder.identity.application.port.in.RegisterLocalUserUseCase;
import br.com.fuelfinder.identity.domain.valueobject.UserId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthRegistrationController.class)
@Import({
        RegistrationHttpExceptionHandler.class,
        WebCorsConfiguration.class
})
class WebCorsConfigurationTest {

    private static final String URL = "/api/v1/auth/register";
    private static final String FRONTEND = "http://localhost:3000";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegisterLocalUserUseCase useCase;

    @Test
    void shouldAllowPreflightFromFrontend() throws Exception {
        mvc.perform(
                        options(URL)
                                .header(HttpHeaders.ORIGIN, FRONTEND)
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        "POST"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                        "Content-Type"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                FRONTEND
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                                containsString("POST")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                                "3600"
                        )
                );
    }

    @Test
    void shouldExposeAllowOriginOnFrontendRequest() throws Exception {
        when(useCase.execute(any(RegisterLocalUserCommand.class)))
                .thenReturn(UserId.newId());

        mvc.perform(
                        post(URL)
                                .header(HttpHeaders.ORIGIN, FRONTEND)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Maria Silva",
                                          "email": "maria@example.com",
                                          "password": "Senha2026"
                                        }
                                        """)
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                FRONTEND
                        )
                );
    }

    @Test
    void shouldRejectPreflightFromUnknownOrigin() throws Exception {
        mvc.perform(
                        options(URL)
                                .header(
                                        HttpHeaders.ORIGIN,
                                        "http://evil.example.com"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        "POST"
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        header().doesNotExist(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                        )
                );
    }

    @Test
    void shouldRejectOtherLocalPorts() throws Exception {
        mvc.perform(
                        options(URL)
                                .header(
                                        HttpHeaders.ORIGIN,
                                        "http://localhost:5173"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        "POST"
                                )
                )
                .andExpect(status().isForbidden());
    }
}
