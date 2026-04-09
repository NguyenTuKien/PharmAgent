package ct01.n07.backend.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("Chưa xác thực: Vui lòng đăng nhập lại để tiếp tục.");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
