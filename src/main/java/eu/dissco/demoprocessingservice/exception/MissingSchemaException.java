package eu.dissco.demoprocessingservice.exception;

public class MissingSchemaException extends SchemaValidationException {

  public MissingSchemaException(String message) {
    super(message);
  }

  public MissingSchemaException(String message, Throwable cause) {
    super(message, cause);
  }
}
