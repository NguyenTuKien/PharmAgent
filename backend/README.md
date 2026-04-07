# PharmAgent Backend API Reference (36 Endpoints)

Tài liệu này cung cấp chi tiết kỹ thuật cho toàn bộ 36 API của hệ thống PharmAgent, chia theo các module nghiệp vụ.

---

## 1. Authentication & Security (`/auth`)

### [API 1] Signup (`POST /auth/signup`)
Đăng ký tài khoản hệ thống.
- **Request Body**:
  | Field | Type | Validation | Description |
  | :--- | :--- | :--- | :--- |
  | `email` | String | Email Format, NotBlank | Email dùng để đăng nhập |
  | `password` | String | Min 8 chars, NotBlank | Mật khẩu |
  | `caregiver` | Object | NotNull | Thông tin Profile của người đăng ký |
- **Output**: `LoginResponse` (accessToken, refreshToken, profiles list).

### [API 2] Login (`POST /auth/login`)
Đăng nhập tài khoản.
- **Request Body**: `{ "email": "...", "password": "..." }`
- **Output**: `LoginResponse`.

### [API 3] Select Profile (`POST /auth/profiles/{id}/select`)
Hoán đổi Account Token lấy Profile Token.
- **Path Variable**: `id` - ID của Profile muốn chọn (profileId).
- **Output**: `{ "profileToken": "ey..." }`

### [API 4] Refresh Token (`POST /auth/refresh`)
Làm mới phiên làm việc.
- **Request Body**: `{ "refreshToken": "...", "profileId": "..." }`
- **Output**: `TokenRefreshResponse` (accessToken, profileToken, refreshToken).

### [API 5] Logout (`POST /auth/logout`)
Đăng xuất tài khoản.
- **Request Body**: `{ "refreshToken": "..." }`
- **Output**: `204 No Content`.

---

## 2. User Profile Module (`/profiles`)

### [API 6] List Account Profiles (`GET /profiles`)
Lấy danh sách Profile thuộc tài khoản hiện tại.
- **Query Params**: `page`, `size`.
- **Output**: `Page<UserProfileSummaryResponse>`.

### [API 7] Get My Profile (`GET /profiles/me`)
Lấy thông tin chi tiết Profile đang truy cập.
- **Output**: `UserProfileResponse` (bao gồm contacts và devices).

### [API 8] Update My Profile (`PUT /profiles/me`)
Cập nhật thông tin cá nhân.
- **Input Fields**: `firstName`, `lastName`, `phone`, `dateOfBirth`, `gender`, `address`.
- **Output**: `UserProfileResponse`.

### [API 9-12] Emergency Contacts (CRUD)
- `GET /profiles/me/contacts`: Lấy danh sách người liên hệ.
- `POST /profiles/me/contacts`: Thêm mới (Body: `name`, `phone`).
- `PUT /profiles/me/contacts/{id}`: Cập nhật (Body: `name`, `phone`).
- `DELETE /profiles/me/contacts/{id}`: Xóa người liên hệ.

### [API 13-16] User Devices (CRUD)
- `GET /profiles/me/devices`: Xem danh sách thiết bị nhận thông báo.
- `POST /profiles/me/devices`: Đăng ký thiết bị (Body: `deviceName`, `deviceToken`, `deviceType`).
- `PUT /profiles/me/devices/{id}`: Cập nhật cấu hình.
- `DELETE /profiles/me/devices/{id}`: Hủy đăng ký.

### [API 17] Create Sub-Profile (`POST /caregiver/profiles`)
**Caregiver** tạo profile Elderly mới trong cùng tài khoản.
- **Input**: `CreateProfileRequest` (firstName, lastName, phone, role, gender...).

### [API 18] Delete Sub-Profile (`DELETE /caregiver/profiles/{id}`)
**Caregiver** xóa một profile phụ. `id` là profileId.

### [API 19] Search Profiles (`POST /caregiver/profiles/search`)
Tìm kiếm Profile khác trong hệ thống để kết nối.
- **Input**: `{ "query": "string", "role": "ELDERLY/CAREGIVER" }`

---

## 3. Relationship Module (`/relationship`)

### [API 20] My Elderly (Accepted) (`GET /caregiver/relationship`)
Danh sách người cao tuổi đang chăm sóc (đã chấp nhận).
- **Output**: `List<ElderlyProfileResponse>` (chứa `relationshipId`, `profileId`, `status`, `permissionLevel`, `firstName`...).

### [API 21] Pending Invites Out (`GET /caregiver/relationship/pending`)
Danh sách lời mời đã gửi đi (đang chờ phản hồi).
- **Output**: `List<ElderlyProfileResponse>` (chứa `relationshipId`, `profileId`, `status`, `permissionLevel`...).

### [API 22] Send Invite (`POST /caregiver/relationship/invite`)
Gửi lời mời kết nối tới Elderly ID qua `profileId`.
- **Input**: `{ "targetElderlyId": "...", "caregiverTitle": "Con trai", "permissionLevel": "EDIT_SCHEDULE" }`
- **Output**: Trả về `relationshipId` (String).

### [API 23] Update Permission (`PATCH /caregiver/relationship/{id}`)
Cập nhật quyền hạn cho mối quan hệ hiện tại. `id` ở đây là `targetElderlyId`.

### [API 24] My Caregivers (Accepted) (`GET /elderly/relationship`)
Danh sách người đang chăm sóc mình (đã chấp nhận).
- **Output**: `List<CaregiverProfileResponse>` (chứa `relationshipId`, `profileId`, `status`, `permissionLevel`...).

### [API 25] Pending Invites In (`GET /elderly/relationship/pending`)
Danh sách lời mời từ phía Caregiver đang chờ Elderly phê duyệt.
- **Output**: `List<CaregiverProfileResponse>` (chứa `relationshipId`, `profileId`, `status`, `permissionLevel`...).

### [API 26] Accept Invite (`PUT /elderly/relationship/{id}/accept`)
Đồng ý lời mời kết nối từ Caregiver. `id` của Path là `relationshipId`.

### [API 27] Refuse Invite (`PUT /elderly/relationship/{id}/refuse`)
Từ chối lời mời. `id` của Path là `relationshipId`.

---

## 4. Pill Catalog Module (`/pills`)

### [API 28] Get Pill Catalog (`GET /pills`)
Danh mục thuốc có phân trang và tìm kiếm theo tên.

### [API 29] Get Pill Detail (`GET /pills/{id}`)
Thông tin chi tiết thuốc (Thành phần, HDSD, Cảnh báo).

### [API 30] Keyword Search (`GET /pills/search`)
Tìm kiếm nhanh theo keyword.

### [API 31] Scan Pill (`POST /pills/scan`)
Nhận diện thuốc qua hình ảnh (MultipartFile).

### [API 32-36] Admin Pill Management
- `POST /admin/pills`: Thêm thuốc mới (PillCreateRequest).
- `PUT /admin/pills/{id}`: Sửa thông tin thuốc.
- `DELETE /admin/pills/{id}`: Xóa thuốc.
- `POST /admin/pills/{id}/images`: Thêm ảnh (PillImageRequest).
- `DELETE /admin/pills/{id}/images/{imgId}`: Xóa ảnh.

---

## 5. Medication & Dose APIs (To Be Finalized)
Các API về đơn thuốc cá nhân (`/medications`), lịch uống thuốc của Caregiver (`/caregiver/medications`) và quản lý liều dùng (`/medications/doses`) hiện đang được xem xét kĩ thuật cuối cùng.
