package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.application.exception.AccountBlockedException;
import br.com.fuelfinder.identity.application.exception.AccountNotVerifiedException;
import br.com.fuelfinder.identity.application.exception.InvalidCredentialsException;
import br.com.fuelfinder.identity.application.exception.InvalidRefreshTokenException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros de autenticacao.
 *
 * Cada resposta traz a propriedade "code", estavel,
 * para o cliente decidir o que mostrar sem depender
 * do texto da mensagem.
 */
@RestControllerAdvice
public class AuthenticationHttpExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Credenciais invalidas",
                exception.getMessage(),
                "INVALID_CREDENTIALS"
        );
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ResponseEntity<ProblemDetail> handleNotVerified(
            AccountNotVerifiedException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "E-mail nao verificado",
                exception.getMessage(),
                "EMAIL_NOT_VERIFIED"
        );
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ProblemDetail> handleBlocked(
            AccountBlockedException exception
    ) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Conta bloqueada",
                exception.getMessage(),
                "ACCOUNT_BLOCKED"
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception
    ) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Sessao invalida",
                exception.getMessage(),
                "INVALID_REFRESH_TOKEN"
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);
        problem.setProperty("code", code);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}
