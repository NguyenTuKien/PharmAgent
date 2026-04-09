package ct01.n07.backend.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super("Không tìm thấy tài nguyên yêu cầu.");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
