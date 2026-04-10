# Danh sách API Hệ thống PharmAgent (Tổng quan)

Tài liệu này liệt kê các endpoint (handler mappings) hiện có trong backend để tham chiếu khi test Postman.

Lưu ý:
- Backend đang chạy với context-path `/api` (xem `application.yaml`). Vì vậy các handler trong controller như `@RequestMapping("/stats")` thực tế sẽ được expose dưới `/api/stats/...`.
- Tổng số handler mappings hiện tại: ~65 (mỗi HTTP method + path được tính riêng). README này liệt kê các endpoint theo module để dễ đọc.

- GHI CHÚ QUAN TRỌNG (tên token & đổi tên entity):
  - Header xác thực chuẩn: `Authorization: Bearer <AuthToken>` (trước đây README dùng từ "AccessToken" ở một vài chỗ — đã chuẩn hóa thành `AuthToken`).
  - Token trả về khi chọn profile (profile-scoped token) được gọi là `accessToken`.
  - Entity / DTO renames đã thực hiện trong codebase: `PatientMedication` -> `Medication`, `MedicationSchedule` -> `MedSchedule`, `ScheduleTime` -> `MedDose`, `EmergencyContact` -> `UserContact`, `DoseEvent` -> `EventDose`.

 | Module / Nhóm | Method | API Endpoint | Chức năng | Input Example (JSON) | Output Example (JSON) | Phân quyền (Auth) |
 | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
 | **1. Xác thực & Phân quyền (Auth)** | POST | `/api/auth/login` | Đăng nhập hệ thống. | `{"email": "test@gmail.com", "password": "password123"}` | `{"authToken": "eyJ...", "refreshToken": "eyJ...", "profiles": [{"profileId": "65c...", "role": "CAREGIVER"}]}` | Public |
 |  | POST | `/api/auth/signup` | Đăng ký tài khoản (tạo User + 1 Profile mặc định). | `{"email": "test@gmail.com", "password": "password123", "confirmPassword": "password123", "caregiver": {...}}` | `{"authToken": "eyJ...", "refreshToken": "eyJ...", "profiles": [...]}` (201 Created) | Public |
 |  | POST | `/api/auth/profiles/{profileId}/select` | Chọn profile để lấy profile-scoped token (`accessToken`). | **Header:** `Bearer <AuthToken>` | `{"accessToken": "eyJ..."}` | Auth |
 |  | POST | `/api/auth/refresh` | Refresh token/profile token. | `{"profileId": "65c...", "refreshToken": "eyJ..."}` | `{"authToken": "eyJ...", "accessToken": "eyJ...", "refreshToken": "eyJ..."}` | Public |
 |  | POST | `/api/auth/logout` | Logout (invalidate refresh token). | `{"refreshToken":"..."}` | `204 No Content` | Auth |
 |  | POST | `/api/auth/forgot-password` | Gửi email lấy lại mật khẩu (OTP). | Query param: `email` | `200 OK` | Public |
 |  | POST | `/api/auth/reset-password` | Reset mật khẩu bằng OTP. | `{"email":"...","otp":"...","newPassword":"...","confirmPassword":"..."}` | `200 OK` | Public |
||
 | **2. Quản lý Profile (Profiles)** | GET | `/api/profiles` | Lấy danh sách profile trong tài khoản. | Header: `Bearer <AuthToken>` | Paginated list | Auth |
 |  | GET | `/api/profiles/me` | Lấy profile hiện tại. | Header: `Bearer <AuthToken>` | `{"profileId":"...","firstName":"...","contacts":[],"devices":[]}` | Auth |
 |  | PUT | `/api/profiles/me` | Cập nhật profile hiện tại. | `{"firstName":"...",...}` | Updated profile | Auth |
 |  | GET | `/api/profiles/me/contacts` | Lấy danh sách user contacts của profile. | Header: `Bearer <AuthToken>` | `[{"id":"c1","name":"Bác sĩ A","phone":"090111222"}]` | Auth |
 |  | POST | `/api/profiles/me/contacts` | Thêm mới user contact. | `{"name":"Bác sĩ A","phone":"090111222"}` | Updated profile (contacts) | Auth |
 |  | PUT | `/api/profiles/me/contacts/{contactId}` | Cập nhật user contact. | `{"name":"Bác sĩ B","phone":"090333444"}` | Updated profile (contacts) | Auth |
 |  | DELETE | `/api/profiles/me/contacts/{contactId}` | Xóa user contact. | - | Updated profile | Auth |
||
 | **3. Thiết bị (Devices)** | GET | `/api/profiles/me/devices` | Lấy danh sách device token của profile. | Header: `Bearer <AuthToken>` | `[{"deviceId":"d1","deviceName":"iPhone 15","type":"MOBILE"}]` | Auth |
 |  | POST | `/api/profiles/me/devices` | Đăng ký device token. | `{"deviceName":"iPhone 15","deviceToken":"Expo[...]","type":"MOBILE"}` | Updated profile (devices) | Auth |
 |  | PUT | `/api/profiles/me/devices/{id}` | Cập nhật device. | `{"deviceName":"iPhone 16",...}` | Updated profile | Auth |
 |  | DELETE | `/api/profiles/me/devices/{id}` | Xóa device. | - | Updated profile | Auth |
||
 | **4. Kết nối Người thân (Relationships)** | GET | `/api/caregiver/relationship` | CAREGIVER: lấy danh sách elderly đang theo dõi. | Header: `Bearer <AuthToken>` | List | CAREGIVER |
 |  | GET | `/api/caregiver/relationship/pending` | CAREGIVER: lấy lời mời đang chờ. | - | List | CAREGIVER |
 |  | POST | `/api/caregiver/relationship/invite` | Gửi lời mời kết nối. | `{"targetElderlyId":"...","caregiverTitle":"...","permissionLevel":"READ_ONLY"}` | `{"id":"r1"}` (201) | CAREGIVER |
 |  | PATCH | `/api/caregiver/relationship/{targetElderlyId}` | Cập nhật permission level. | RequestParam `permissionLevel` | `200 OK` | CAREGIVER |
 |  | GET | `/api/elderly/relationship` | ELDERLY: lấy danh sách caregiver được chấp nhận. | - | List | ELDERLY |
 |  | GET | `/api/elderly/relationship/pending` | ELDERLY: lấy lời mời đang chờ. | - | List | ELDERLY |
 |  | PUT | `/api/elderly/relationship/{id}/accept` | ELDERLY: chấp nhận lời mời. | - | 200 OK | ELDERLY |
 |  | PUT | `/api/elderly/relationship/{id}/refuse` | ELDERLY: từ chối lời mời. | - | 200 OK | ELDERLY |
||
 | **5. Messages** | POST | `/api/messages` | Gửi tin nhắn (ví dụ thông báo). | `{"toProfileId":"...","title":"...","body":"..."}` | Created message | Auth |
 |  | GET | `/api/messages` | Lấy danh sách tin nhắn của user (paginated). | Pageable | Page<MessageResponse> | Auth |
||
 | **6. Events / Doses** | GET | `/api/events/today` | Lấy events (doses) hôm nay cho patientId. | `?patientId=...` | Page<EventDoseResponse> | Auth |
 |  | GET | `/api/events/pending` | Lấy pending doses. | `?patientId=...` | Page | Auth |
 |  | GET | `/api/events/processed` | Lấy processed doses. | `?patientId=...` | Page | Auth |
 |  | POST | `/api/elderly/events/{id}/confirm` | ELDERLY: confirm dose taken. | - | EventDoseResponse | ELDERLY |
 |  | PUT | `/api/caregiver/events/{id}/status` | CAREGIVER: cập nhật trạng thái dose thủ công. | `{"status":"TAKEN"}` | EventDoseResponse | CAREGIVER |
||
 | **7. Medications** | POST | `/api/caregiver/medications` | (CAREGIVER) Tạo medication cho patient (kèm schedules). | MedicationCreateRequest | MedicationResponse (201) | CAREGIVER |
 |  | PUT | `/api/caregiver/medications/{id}` | (CAREGIVER) Cập nhật medication | MedicationUpdateRequest | MedicationResponse | CAREGIVER |
 |  | DELETE | `/api/caregiver/medications/{id}` | (CAREGIVER) Xóa medication | - | 204 No Content | CAREGIVER |
 |  | POST | `/api/caregiver/medications/{id}/schedules` | (CAREGIVER) Thêm schedule cho medication | MedScheduleRequest | MedicationResponse | CAREGIVER |
 |  | PUT | `/api/caregiver/medications/{id}/schedules/{scheduleId}` | (CAREGIVER) Cập nhật schedule | MedScheduleRequest | MedicationResponse | CAREGIVER |
 |  | DELETE | `/api/caregiver/medications/{id}/schedules/{scheduleId}` | (CAREGIVER) Xóa schedule | - | MedicationResponse | CAREGIVER |
 |  | POST | `/api/caregiver/medications/{id}/schedules/{scheduleId}/times` | (CAREGIVER) Thêm thời điểm (dose time) vào schedule | MedDoseRequest | MedicationResponse | CAREGIVER |
 |  | PUT | `/api/caregiver/medications/{id}/schedules/{scheduleId}/times/{timeId}` | (CAREGIVER) Cập nhật thời điểm uống thuốc | MedDoseRequest | MedicationResponse | CAREGIVER |
 |  | DELETE | `/api/caregiver/medications/{id}/schedules/{scheduleId}/times/{timeId}` | (CAREGIVER) Xóa thời điểm uống thuốc | - | MedicationResponse | CAREGIVER |
 |  | GET | `/api/medications?patientId=...` | Lấy medications (optionally filter isActive). | `?patientId=...&isActive=true` | Page<MedicationResponse> | Auth |
 |  | GET | `/api/medications/{id}` | Lấy detail medication by id. | - | MedicationResponse | Auth |
||
 | **8. Pills (Catalog & AI scan)** | GET | `/api/pills` | Danh mục thuốc (paginated, optional search). | `?search=...` | Page<PillCatalogResponse> | Auth |
 |  | GET | `/api/pills/{id}` | Chi tiết thuốc. | - | Pill | Auth |
 |  | GET | `/api/pills/search` | Tìm nhanh theo keyword. | `?keyword=Panadol` | List<Pill> | Auth |
 |  | POST | `/api/pills/scan` | Scan ảnh thuốc (multipart file). | Multipart file | PillScanResponse | Auth |
||
 | **9. Admin (CMS)** | POST | `/api/admin/pills` | Thêm thuốc (CMS). | PillCreateRequest | Created PillResponse | ADMIN |
 |  | PUT | `/api/admin/pills/{id}` | Cập nhật thuốc. | PillRequest | PillResponse | ADMIN |
 |  | DELETE | `/api/admin/pills/{id}` | Xóa thuốc. | - | 200 OK | ADMIN |
 |  | POST | `/api/admin/pills/{pillId}/images` | Thêm hình ảnh thuốc. | PillImageRequest | PillResponse | ADMIN |
 |  | DELETE | `/api/admin/pills/{pillId}/images/{imageId}` | Xóa hình ảnh thuốc. | - | 200 OK | ADMIN |
 |  | GET | `/api/admin/users` | (Admin) Lấy danh sách users. | Pageable | Page<AdminUserResponse> | ADMIN |
 |  | POST | `/api/admin/users` | Tạo user admin. | AdminUserCreateRequest | AdminUserResponse (201) | ADMIN |
 |  | PUT | `/api/admin/users/{id}` | Cập nhật user. | AdminUserUpdateRequest | AdminUserResponse | ADMIN |
 |  | DELETE | `/api/admin/users/{id}` | Xóa user. | - | 204 No Content | ADMIN |
 |  | PATCH | `/api/admin/users/{id}/lock` | Khóa user. | - | AdminUserResponse | ADMIN |
 |  | PATCH | `/api/admin/users/{id}/unlock` | Mở khóa user. | - | AdminUserResponse | ADMIN |
||
 | **10. Stats & Reports** | GET | `/api/stats/adherence` | Tính tỷ lệ tuân thủ uống thuốc cho 1 patient trong khoảng ngày. | `?patientId=...&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | Ví dụ: `{"patientId":"65c...","adherenceRate":0.87,"taken":87,"expected":100}` | Auth |
 |  | GET | `/api/stats/doses-by-medication` | Thống kê số lượng liều đã được đánh dấu TAKEN theo từng loại thuốc trong khoảng thời gian (dùng để vẽ biểu đồ consumption). Parameters: `patientId` (String), `startDate` (ISO date YYYY-MM-DD), `endDate` (ISO date YYYY-MM-DD). Response: List<MedicationDoseStatsResponse> e.g. `[ {"nickname":"Panadol","takenCount":42}, {"nickname":"Aspirin","takenCount":15} ]` | `?patientId=...&startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | Ví dụ: `[ {"nickname":"Panadol","takenCount":42}, {"nickname":"Aspirin","takenCount":15} ]` | Auth |
 |  | GET | `/api/stats/inventory-warnings` | Trả về danh sách cảnh báo tồn kho (sắp hết hoặc hết) cho patientId. | `?patientId=...` | Ví dụ: `[ {"medicationId":"m1","pillName":"Panadol","remaining":2,"threshold":3}, ... ]` | Auth |
