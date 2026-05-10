package ct01.n07.backend.dto.upload;

import lombok.Builder;
import lombok.Data;

/**
 * Response trả về cho FE để upload ảnh trực tiếp lên Cloudinary.
 * FE sẽ dùng các trường này để gửi multipart POST đến Cloudinary upload API.
 */
@Data
@Builder
public class PresignedUploadResponse {

    /** Cloudinary upload endpoint */
    private String uploadUrl;

    /** Tên cloud */
    private String cloudName;

    /** API Key (public) */
    private String apiKey;

    /** Folder lưu ảnh trên Cloudinary */
    private String folder;

    /** Unix timestamp hết hạn signature */
    private long timestamp;

    /** SHA-1 signature để xác thực upload */
    private String signature;
}
