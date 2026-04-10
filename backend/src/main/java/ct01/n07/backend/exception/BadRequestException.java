package ct01.n07.backend.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException() {
        super("Dữ liệu đầu vào hoặc tham số không hợp lệ.");
    }

    public BadRequestException(String message) {
        super(message);
    }
}
