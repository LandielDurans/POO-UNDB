package br.com.fuelfinder.identity.adapter.out.email;

import br.com.fuelfinder.identity.application.model.PreparedVerificationEmail;
import br.com.fuelfinder.identity.application.port.out.VerificationEmailSender;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class SmtpVerificationEmailSender
        implements VerificationEmailSender {

    static final String SUBJECT =
            "Confirme seu e-mail no FuelFinder";

    private static final DateTimeFormatter EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
                    .withZone(ZoneId.of("America/Sao_Paulo"));

    private final JavaMailSender mailSender;
    private final String from;
    private final String linkBaseUrl;

    public SmtpVerificationEmailSender(
            JavaMailSender mailSender,
            String from,
            String linkBaseUrl
    ) {
        this.mailSender = Objects.requireNonNull(mailSender);

        this.from = requireText(
                from,
                "Remetente nao pode ser vazio."
        );

        this.linkBaseUrl = requireText(
                linkBaseUrl,
                "URL do link de verificacao nao pode ser vazia."
        );
    }

    @Override
    public void send(PreparedVerificationEmail email) {
        Objects.requireNonNull(
                email,
                "Email preparado nao pode ser nulo."
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email.recipient().value());
        message.setSubject(SUBJECT);
        message.setText(body(email));

        // O token bruto existe apenas no corpo da mensagem;
        // nunca deve ser registrado em logs.
        mailSender.send(message);
    }

    String verificationLink(String token) {
        return linkBaseUrl
                + (linkBaseUrl.contains("?") ? "&" : "?")
                + "token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String body(PreparedVerificationEmail email) {
        return """
                Olá!

                Recebemos seu cadastro no FuelFinder. Para ativar sua conta, \
                confirme seu e-mail pelo link abaixo:

                %s

                O link é válido até %s (horário de Brasília).

                Se você não fez este cadastro, ignore este e-mail.

                Equipe FuelFinder
                """.formatted(
                verificationLink(email.token()),
                EXPIRATION_FORMAT.format(email.expiresAt())
        );
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);

        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
