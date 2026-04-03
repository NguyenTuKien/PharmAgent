# PharmAgent Backend API

Tai lieu nay mo ta cac API hien tai trong backend (dua tren code controller hien co).

## Base URL

- Local: `http://localhost:8080`
- Context path: `/api`
- Vi du full path: `http://localhost:8080/api/auth/login`

## Auth/Security

Theo `SecurityConfiguration`:

- Public (khong can JWT): `/api/auth/**`
- Can xac thuc JWT: tat ca route con lai (`/api/pills/**`, `/api/patient-medications/**`, ...)

---

## 1) Auth APIs (`/api/auth`)

### 1.1 Login account

- **Endpoint**: `POST /api/auth/login`
- **Chuc nang**: Dang nhap bang email/password, tra `accessToken`, `refreshToken`, va danh sach profile (phan trang).
- **Query params (pagination)**:
  - `page` (default `0`)
  - `size` (default `10`)
  - `sort` (optional)
- **Input body** (`LoginRequest`):

```json
{
  "email": "user@example.com",
  "password": "Secret@123"
}
```

- **Output** (`LoginResponse`):

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "profiles": {
    "content": [
      {
        "id": "profileId",
        "phone": "090...",
        "firstName": "An",
        "lastName": "Nguyen",
        "avatarUrl": "https://...",
        "role": "ELDERLY"
      }
    ],
    "number": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 1.2 Select profile (token exchange)

- **Endpoint**: `POST /api/auth/select-profile`
- **Chuc nang**: Doi `accessToken` cap account thanh `profileToken` theo profile duoc chon.
- **Headers**:
  - `Authorization: Bearer <accessToken>`
- **Input body**:

```json
{
  "profileId": "string"
}
```

- **Output**:

```json
{
  "profileToken": "..."
}
```

### 1.3 Refresh token

- **Endpoint**: `POST /api/auth/refresh`
- **Chuc nang**: Rotate refresh token va cap lai `profileToken`.
- **Input body** (`TokenRefreshRequest`):

```json
{
  "refreshToken": "string",
  "profileId": "string"
}
```

- **Output** (`TokenRefreshResponse`):

```json
{
  "profileToken": "...",
  "refreshToken": "..."
}
```

### 1.4 Logout

- **Endpoint**: `POST /api/auth/logout`
- **Chuc nang**: Huy refresh token hien tai (set null trong DB).
- **Input body**:

```json
{
  "refreshToken": "string"
}
```

- **Output**: `204 No Content`

---

## 2) Pill APIs (`/api/pills`)

### 2.1 Get all pills (pagination)

- **Endpoint**: `GET /api/pills`
- **Chuc nang**: Lay danh sach thuoc dang active theo trang.
- **Query params**:
  - `page` (default `0`)
  - `size` (default `10`)
  - `sort` (optional)
- **Output**: `Page<Pill>`

### 2.2 Add new pill

- **Endpoint**: `POST /api/pills`
- **Chuc nang**: Tao thuoc moi.
- **Input body** (`PillRequest`):
  - `name`, `genericName`, `brandName`, `strength`, `dosageForm`, `color`, `shape`, `description`, `usageInstructions`, `warning`, `sideEffects`, `manufacturer`
- **Output**: `201 Created` + message

### 2.3 Get pill by id

- **Endpoint**: `GET /api/pills/{id}`
- **Chuc nang**: Lay chi tiet thuoc theo id.
- **Output**: `Pill`

### 2.4 Search pills

- **Endpoint**: `GET /api/pills/search?keyword=...`
- **Chuc nang**: Tim thuoc theo tu khoa.
- **Output**: `List<Pill>`

### 2.5 Update pill

- **Endpoint**: `PUT /api/pills/{id}`
- **Chuc nang**: Cap nhat thong tin thuoc.
- **Input body**: `PillRequest`
- **Output**: message thanh cong

### 2.6 Delete pill

- **Endpoint**: `DELETE /api/pills/{id}`
- **Chuc nang**: Xoa (hoac deactive) thuoc theo service implementation.
- **Output**: message thanh cong

### 2.7 Add image for pill

- **Endpoint**: `POST /api/pills/{pillId}/images`
- **Chuc nang**: Them hinh anh cho thuoc.
- **Input body** (`PillImageRequest`):

```json
{
  "imageUrl": "https://...",
  "viewType": "FRONT",
  "isPrimary": true
}
```

- **Output**: `201 Created` + message

---

## 3) Patient Medication APIs (`/api/patient-medications`)

### 3.1 Get all patient medications

- **Endpoint**: `GET /api/patient-medications`
- **Chuc nang**: Lay tat ca don thuoc benh nhan.
- **Output**: `List<PatientMedication>`

### 3.2 Get patient medication by id

- **Endpoint**: `GET /api/patient-medications/{id}`
- **Chuc nang**: Lay chi tiet don thuoc.
- **Output**: `PatientMedication`

### 3.3 Get medications by patientId

- **Endpoint**: `GET /api/patient-medications/patient/{patientId}`
- **Chuc nang**: Lay cac don thuoc cua mot benh nhan.
- **Output**: `List<PatientMedication>`

### 3.4 Create patient medication

- **Endpoint**: `POST /api/patient-medications`
- **Chuc nang**: Tao don thuoc moi cho benh nhan.
- **Input body** (`PatientMedicationRequest`):
  - `patientId`, `pillId`, `nickname`, `dosageAmount`, `dosageUnit`, `route`, `mealRelation`, `instruction`, `prescribedBy`, `purpose`, `startDate`, `endDate`, `isPrn`, `maxPerDay`
- **Output**: `201 Created` + `PatientMedication`

### 3.5 Update patient medication

- **Endpoint**: `PUT /api/patient-medications/{id}`
- **Chuc nang**: Cap nhat don thuoc.
- **Input body**: `PatientMedicationRequest`
- **Output**: `PatientMedication`

### 3.6 Delete patient medication

- **Endpoint**: `DELETE /api/patient-medications/{id}`
- **Chuc nang**: Xoa don thuoc.
- **Output**: `204 No Content`

### 3.7 Add schedule

- **Endpoint**: `POST /api/patient-medications/{id}/schedules`
- **Chuc nang**: Them lich uong thuoc vao don thuoc.
- **Input body** (`MedicationScheduleRequest`):
  - `scheduleType`, `frequencyInterval`, `daysOfWeek`, `reminderEnabled`, `reminderMinutesBefore`, `note`, `startDate`, `endDate`, `scheduleTimeRequests`
- **Output**: `PatientMedication`

### 3.8 Update schedule

- **Endpoint**: `PUT /api/patient-medications/{id}/schedules/{scheduleId}`
- **Chuc nang**: Cap nhat 1 lich trong don thuoc.
- **Input body**: `MedicationScheduleRequest`
- **Output**: `PatientMedication`

### 3.9 Delete schedule

- **Endpoint**: `DELETE /api/patient-medications/{id}/schedules/{scheduleId}`
- **Chuc nang**: Xoa 1 lich trong don thuoc.
- **Output**: `PatientMedication`

### 3.10 Add schedule time

- **Endpoint**: `POST /api/patient-medications/{id}/schedules/{scheduleId}/times`
- **Chuc nang**: Them moc gio uong thuoc vao lich.
- **Input body** (`ScheduleTimeRequest`):

```json
{
  "takenTime": "08:00:00",
  "quantity": 1
}
```

- **Output**: `PatientMedication`

### 3.11 Update schedule time

- **Endpoint**: `PUT /api/patient-medications/{id}/schedules/{scheduleId}/times/{timeId}`
- **Chuc nang**: Cap nhat moc gio trong lich.
- **Input body**: `ScheduleTimeRequest`
- **Output**: `PatientMedication`

### 3.12 Delete schedule time

- **Endpoint**: `DELETE /api/patient-medications/{id}/schedules/{scheduleId}/times/{timeId}`
- **Chuc nang**: Xoa moc gio trong lich.
- **Output**: `PatientMedication`

---

## Ghi chu

- Cac API co `@Valid` se validate theo annotation trong DTO (NotBlank, NotNull, Size, Min/Max...).
- Response loi chi tiet phu thuoc vao `ResponseStatusException` va global exception handling (neu co).
- Neu can, co the tach them `docs/API-EXAMPLES.md` de luu sample request/response day du cho FE test.
