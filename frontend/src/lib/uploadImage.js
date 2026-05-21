import { apiClient } from './apiClient.js'

/**
 * Upload ảnh lên Cloudinary thông qua presign flow:
 * 1. Gọi backend lấy presigned credentials
 * 2. POST multipart thẳng đến Cloudinary
 * 3. Trả về secure_url
 *
 * @param {File} file - File ảnh cần upload
 * @param {'avatar'|'pill'} folder - Folder đích trên Cloudinary
 * @returns {Promise<string>} secure_url của ảnh
 */
export async function uploadImageToCloudinary(file, folder = 'pill') {
  // Bước 1: Lấy presign từ backend
  const { data: presign } = await apiClient.get('/upload/presign', {
    params: { folder },
  })

  // Bước 2: Build multipart form
  const form = new FormData()
  form.append('file', file)
  form.append('api_key', presign.apiKey)
  form.append('timestamp', presign.timestamp)
  form.append('signature', presign.signature)
  form.append('folder', presign.folder)

  // Bước 3: POST trực tiếp đến Cloudinary (không qua backend)
  const response = await fetch(presign.uploadUrl, {
    method: 'POST',
    body: form,
  })

  if (!response.ok) {
    const err = await response.json().catch(() => ({}))
    throw new Error(err?.error?.message ?? `Cloudinary upload failed: ${response.status}`)
  }

  const result = await response.json()
  return result.secure_url
}
