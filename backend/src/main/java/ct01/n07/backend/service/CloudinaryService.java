package ct01.n07.backend.service;

import ct01.n07.backend.dto.upload.PresignedUploadResponse;

public interface CloudinaryService {

    /**
     * Tạo thông tin presigned để FE upload ảnh trực tiếp lên Cloudinary.
     *
     * @param folder "avatar", "pill" hoặc "chat"
     * @return PresignedUploadResponse chứa uploadUrl, signature, timestamp, ...
     */
    PresignedUploadResponse generatePresignedUpload(String folder);
}
