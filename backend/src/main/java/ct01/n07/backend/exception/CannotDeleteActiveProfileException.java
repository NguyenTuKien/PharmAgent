package ct01.n07.backend.exception;

/**
 * Ném khi caregiver cố xóa profile đang được chọn (active profile).
 */
public class CannotDeleteActiveProfileException extends RuntimeException {

    public CannotDeleteActiveProfileException() {
        super("Không thể xóa profile đang được sử dụng. Vui lòng chuyển sang profile khác trước.");
    }

    public CannotDeleteActiveProfileException(String message) {
        super(message);
    }
}
