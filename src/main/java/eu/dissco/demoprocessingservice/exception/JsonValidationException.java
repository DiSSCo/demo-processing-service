package eu.dissco.demoprocessingservice.exception;

public class JsonValidationException extends SchemaValidationException {

    public JsonValidationException(String message) {
        super(message);
    }
}
