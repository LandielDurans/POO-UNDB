package br.com.fuelfinder.identity.adapter.in.web;

import br.com.fuelfinder.identity.application.exception.EmailAlreadyRegisteredException;

import br.com.fuelfinder.identity.application.exception.InvalidEmailVerificationTokenException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice
public class RegistrationHttpExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(
            Exception exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisicao invalida",
                "Revise os dados enviados e tente novamente."
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            EmailAlreadyRegisteredException exception
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflito de cadastro",
                "Nao foi possivel concluir o cadastro."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDatabaseConflict(
            DataIntegrityViolationException exception
    ) {
        if (isUniqueViolation(exception)) {
            return problem(
                    HttpStatus.CONFLICT,
                    "Conflito de cadastro",
                    "Nao foi possivel concluir o cadastro."
            );
        }

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Nao foi possivel processar a solicitacao."
        );
    }

    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(
            InvalidEmailVerificationTokenException exception
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Token invalido",
                "O token de verificacao e invalido ou expirou."
        );
    }

    private boolean isUniqueViolation(Throwable exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(title);

        return ResponseEntity
                .status(status)
                .body(problem);
    }
}
