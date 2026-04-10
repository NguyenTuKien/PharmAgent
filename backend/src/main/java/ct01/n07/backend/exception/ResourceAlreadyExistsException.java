package ct01.n07.backend.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException() {
        super("Tài nguyên hoặc thực thể này đã tồn tại trong hệ thống.");
    }

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
