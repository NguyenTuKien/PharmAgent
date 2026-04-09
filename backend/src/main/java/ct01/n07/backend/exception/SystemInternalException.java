package ct01.n07.backend.exception;

public class SystemInternalException extends RuntimeException {
    public SystemInternalException() {
        super("Đã xảy ra lỗi hệ thống nội bộ. Vui lòng liên hệ quản trị viên.");
    }

    public SystemInternalException(String message) {
        super(message);
    }
}
