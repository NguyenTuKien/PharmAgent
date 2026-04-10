package ct01.n07.backend.exception;

public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("Từ chối truy cập: Bạn không có quyền thực hiện hành động này.");
    }

    public ForbiddenAccessException(String message) {
        super(message);
    }
}
