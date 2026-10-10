package br.com.fuelfinder.identity.adapter.in.web;

public class RegistrationHttpExceptionHandler extends RuntimeException {
  public RegistrationHttpExceptionHandler(String message) {
    super(message);
  }
}
