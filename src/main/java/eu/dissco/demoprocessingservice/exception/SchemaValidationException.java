package eu.dissco.demoprocessingservice.exception;

public class SchemaValidationException extends Exception {

  public SchemaValidationException(String message) {
    super(message);
  }

  public SchemaValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
