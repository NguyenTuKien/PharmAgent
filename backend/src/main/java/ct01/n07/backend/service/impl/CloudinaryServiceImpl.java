package ct01.n07.backend.service.impl;

import com.cloudinary.Cloudinary;
import ct01.n07.backend.dto.upload.PresignedUploadResponse;
import ct01.n07.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    private static final long SIGNATURE_EXPIRY_SECONDS = 3600L; // 1 giờ

    @Override
    public PresignedUploadResponse generatePresignedUpload(String folder) {
        String resolvedFolder = resolveFolder(folder);
        long timestamp = System.currentTimeMillis() / 1000L;

        Map<String, Object> params = Map.of(
                "timestamp", timestamp,
                "folder",    resolvedFolder
        );

        try {
            String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);

            return PresignedUploadResponse.builder()
                    .uploadUrl("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .cloudName(cloudName)
                    .apiKey(apiKey)
                    .folder(resolvedFolder)
                    .timestamp(timestamp)
                    .signature(signature)
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo presigned upload: " + e.getMessage());
        }
    }

    /** Kiểm tra folder hợp lệ và trả về tên folder trên Cloudinary */
    private String resolveFolder(String folder) {
        return switch (folder == null ? "" : folder.toLowerCase()) {
            case "avatar", "avatars" -> "pharmagent/avatars";
            case "pill",   "pills"   -> "pharmagent/pills";
            case "chat",   "chats"   -> "pharmagent/chat";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Folder không hợp lệ. Chỉ chấp nhận: avatar, pill, chat");
        };
    }
}
