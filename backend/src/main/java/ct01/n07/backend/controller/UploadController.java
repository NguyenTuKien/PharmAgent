package ct01.n07.backend.controller;

import ct01.n07.backend.dto.upload.PresignedUploadResponse;
import ct01.n07.backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp presigned upload URL để FE tự upload ảnh lên Cloudinary.
 *
 * Luồng:
 *  1. FE gọi GET /upload/presign?folder=avatar  (hoặc pill)
 *  2. Backend trả về { uploadUrl, cloudName, apiKey, folder, timestamp, signature }
 *  3. FE POST multipart thẳng đến Cloudinary với các trường trên
 *  4. Cloudinary trả về { secure_url, ... } – FE dùng URL đó gọi tiếp API cập nhật
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/upload")
public class UploadController {

    private final CloudinaryService cloudinaryService;

    /**
     * Lấy thông tin presigned để upload ảnh.
     *
     * @param folder "avatar" hoặc "pill"
     */
    @GetMapping("/presign")
    public ResponseEntity<PresignedUploadResponse> getPresignedUpload(
            @RequestParam String folder) {
        return ResponseEntity.ok(cloudinaryService.generatePresignedUpload(folder));
    }
}
