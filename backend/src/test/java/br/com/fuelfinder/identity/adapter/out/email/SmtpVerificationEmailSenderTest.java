package br.com.fuelfinder.identity.adapter.out.email;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.domain.valueobject.Email;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmtpVerificationEmailSenderTest {

    private static final String FROM =
            "FuelFinder <no-reply@fuelfinder.local>";

    private static final String LINK_BASE =
            "http://localhost:3000/verificar-email";

    // 2026-10-31 15:00 UTC = 12:00 em Brasilia.
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-10-31T15:00:00Z");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private final SmtpVerificationEmailSender sender =
            new SmtpVerificationEmailSender(mailSender, FROM, LINK_BASE);

    @Test
    void shouldSendVerificationLinkToRecipient() {
        sender.send(prepared("abc_DEF-123"));

        SimpleMailMessage message = captureMessage();

        assertEquals(FROM, message.getFrom());
        assertArrayEquals(
                new String[]{"maria@example.com"},
                message.getTo()
        );
        assertEquals(
                SmtpVerificationEmailSender.SUBJECT,
                message.getSubject()
        );
        assertTrue(
                message.getText().contains(
                        LINK_BASE + "?token=abc_DEF-123"
                )
        );
        assertTrue(
                message.getText().contains("31/10/2026 às 12:00")
        );
    }

    @Test
    void shouldEncodeTokenInLink() {
        assertEquals(
                LINK_BASE + "?token=a%2Bb%3Dc",
                sender.verificationLink("a+b=c")
        );
    }

    @Test
    void shouldAppendTokenToExistingQuery() {
        SmtpVerificationEmailSender withQuery =
                new SmtpVerificationEmailSender(
                        mailSender,
                        FROM,
                        LINK_BASE + "?origem=email"
                );

        assertEquals(
                LINK_BASE + "?origem=email&token=abc",
                withQuery.verificationLink("abc")
        );
    }

    @Test
    void shouldPropagateSmtpFailure() {
        doThrow(new MailSendException("SMTP indisponivel"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // O caso de uso trata a falha com retry.
        assertThrows(
                MailSendException.class,
                () -> sender.send(prepared("abc"))
        );
    }

    @Test
    void shouldRejectBlankConfiguration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SmtpVerificationEmailSender(
                        mailSender, " ", LINK_BASE
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SmtpVerificationEmailSender(
                        mailSender, FROM, ""
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new SmtpVerificationEmailSender(
                        mailSender, FROM, null
                )
        );
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(
                NullPointerException.class,
                () -> sender.send(null)
        );

        verifyNoInteractions(mailSender);
    }

    private SimpleMailMessage captureMessage() {
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(captor.capture());

        return captor.getValue();
    }

    private static PreparedVerificationEmail prepared(String token) {
        return new PreparedVerificationEmail(
                Email.of("maria@example.com"),
                token,
                EXPIRES_AT
        );
    }
}
