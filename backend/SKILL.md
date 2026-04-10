---
name: Một số thống nhất chung cho Backend
description: Tiêu chuẩn lập trình và kiến trúc thư mục Backend Spring Boot thực tế đóng gói bằng Facade, Service, Repository (Dự án PillLen / PharmAgent)
---

# Một số thống nhất cho Backend (PillLen / PharmAgent)

Tài liệu này quy định các tiêu chuẩn lập trình, kiến trúc thư mục và các quy tắc bắt buộc dành cho Team Backend dựa trên thiết kế hiện tại của dự án PillLen (Hệ thống nhắc nhở và quản lý uống thuốc Multi-Profile).

## 1. Công Nghệ Sử Dụng (Tech Stack)
- **Java**: Java 17+
- **Framework Chính**: Spring Boot 3.x
- **Database**: MongoDB (NoSQL) thông qua Spring Data MongoDB
- **Caching**: Redis (Quản lý Token Blacklist & Caching)
- **Security**: JWT Authentication + Spring Security
- **Mapping Data**: MapStruct
- **Boilerplate**: Lombok

## 2. Sơ Đồ Cây Thư Mục (Folder Tree)
Dưới đây là cây thư mục chuẩn đang triển khai trong code. Bất kỳ tính năng mới nào cũng phải tuân theo đúng luồng thư mục này:

```text
src/main/java/ct01/web/backend/
├── config/           # Các file cấu hình hệ thống (Security, Redis, MongoDB, DataSeeder)
├── constant/         # CHÚ Ý: Nơi chứa hằng số dự án (Status, Error Messages...) (Nên bổ sung)
├── controller/       # Rest API endpoints (*Controller.java). Chỉ gọi Facade hoặc Service.
│   ├── caregiver/    # Phân quyền riêng (Nếu có)
│   └── ...           # Các endpoint nghiệp vụ (Pill, Medication, Auth...)
├── dto/              # Data Transfer Object dùng để nhận Request và trả Response.
│   ├── auth/         # DTO chia theo từng domain nghiệp vụ
│   ├── doseEvent/
│   └── ...
├── exception/        # Nơi chứa các class lỗi Custom và RestExceptionHandler (Nên bổ sung)
├── facade/           # CHÚ Ý: Bộ điều phối gom logic chéo của nhiều Service (VD: AuthFacade)
├── filter/           # Lớp chặn request (VD: JwtAuthenticationFilter)
├── mapper/           # Tầng chuyển đổi dữ liệu DTO <-> Model sử dụng MapStruct
├── model/            # Document ánh xạ xuống MongoDB (Các class có @Document)
│   └── enums/        # Các Enum dùng trong DB (Role, Gender, DeviceType...)
├── repository/       # Interface truy xuất MongoDB (Kế thừa MongoRepository)
├── security/         # Provider, JwtService và các xử lý lõi của Security
├── service/          # Chứa Interface định nghĩa nghiệp vụ
│   └── impl/         # Chứa Class thực thi chi tiết (Hậu tố *ServiceImpl)
└── util/             # Nơi chứa các hàm tiện ích tĩnh (JwtUtil)
```

## 3. Kiến Trúc Lõi Bắt Buộc Tuân Thủ

### 3.1. Thứ tự gọi tầng (Layered Flow)
- **Quy tắc:** Mũi tên 1 chiều: `Controller` -> `Facade` (nếu có) -> `Service` -> `Repository` -> `Model`.
- **CẤM:** Không được gọi trực tiếp `Repository` hoặc `Model` từ `Controller`. Không để logic nghiệp vụ (If/Else) lọt lên tầng Controller.

### 3.2. Facade Pattern (Bộ Điều Phối)
- Dự án áp dụng tầng **Facade** (`facade/`) để điều phối các service.
- **Quy tắc:** Nếu 1 API cần xử lý logic liên quan đến từ 2 Service trở lên (Ví dụ: `AuthFacade` cần gọi cả `UserService`, `UserProfileService` và `JwtService`), **bắt buộc** phải tạo Facade để đóng gói logic. Không tiêm (inject) Service này vào Service kia để tránh Dependency Cycle (Vòng lặp phụ thuộc).

### 3.3. Giao Diện Service (Interface & Impl)
- Khác với một số dự án viết gộp, PillLen tách biệt rõ ràng Interface và Implementation.
- Tất cả nghiệp vụ phải được định nghĩa tại `service/*Service.java` và code chi tiết tại `service/impl/*ServiceImpl.java`.

### 3.4. Repository & Model (NoSQL Standards)
- Các file tương tác với DB nằm ở `repository/` và kết thúc bằng đuôi `*Repository` (VD: `PillRepository`). **Tuyệt đối không dùng hậu tố Dao.**
- Các Entity đại diện cho DB nằm ở `model/` (VD: `UserProfile`). Các Object bị nhúng (Embedded) bên trong Document cần có ID tự sinh: `@Builder.Default private String id = new ObjectId().toString();`.
- **CẤM:** Tuyệt đối không được sửa đổi cấu trúc các class trong tầng `model/` (ví dụ: thêm/bớt các thuộc tính của `UserProfile`, `PatientMedication`...) trừ khi có sự thống nhất của cả team. Mọi thay đổi dữ liệu yêu cầu từ Client **phải được giải quyết thông qua việc tạo mới các DTO**. Chỉ định nghĩa DTO nếu Object đó có chứa nhiều Attribute, tránh việc tạo DTO tràn lan cho các Request chỉ có 1-2 tham số.

### 3.5. Quản Lý Lỗi & Biến Toàn Cục
- Ném lỗi bằng `ResponseStatusException` (hoặc Custom Exception) để Global Exception Handler tự động bắt và trả JSON chuẩn cho FE. Không dùng `try-catch` cục bộ trả về `ResponseEntity` lỗi ở Controller.
- **CẤM** hard-code các chuỗi String (Ví dụ: `"User không tồn tại"`) lặp đi lặp lại. Hãy tạo các class tĩnh trong thư mục `constant/` để tái sử dụng.

### 3.6. Dependency Injection & Boilerplate
- **Tuyệt đối không** dùng `@Autowired` trên field. Bắt buộc dùng Constructor Injection thông qua `@RequiredArgsConstructor` của thư viện Lombok.
- Tận dụng tối đa `@Data`, `@Builder`, `@Slf4j` để giảm thiểu code thừa.

### 3.7. MapStruct (Bắt buộc)
- Mọi thao tác chuyển đổi dữ liệu giữa Request DTO -> Model hoặc Model -> Response DTO đều phải thực hiện thông qua Interface nằm trong `mapper/` sử dụng `@Mapper(componentModel = "spring")`. Không mapping thủ công bằng Builder trong Controller.

### 3.8. Quy tắc Auth & Multi-Profile (Đặc thù dự án)
Hệ thống sử dụng mô hình 1 Tài khoản - Nhiều Hồ sơ. Phân biệt rõ:
- **Access Token:** Trả về lúc Login. Chỉ dùng để lấy danh sách Profile hoặc tạo Profile mới.
- **Profile Token:** Trả về khi gọi `/auth/select-profile`. Chứa `profileId` và `role`.
- **Quy tắc truy xuất:** Mọi API nghiệp vụ (lấy đơn thuốc, cập nhật cữ thuốc...) **bắt buộc** phải dùng Profile Token. Khi cần định danh, gọi hàm `userProfileService.getCurrentUserProfile()` để lấy chính xác hồ sơ đang hoạt động.

## 4. Flow Viết Tiêu Chuẩn 1 Tính Năng
1. Thiết kế Document DB lồng nhau (Embedded) -> Code `Xxx.java` trong `model/`.
2. Định nghĩa Interface truy xuất dữ liệu -> `XxxRepository.java` trong `repository/`.
3. Tạo các class Data Transfer -> `XxxRequest.java` / `XxxResponse.java` trong `dto/`.
4. Tạo Interface ánh xạ dữ liệu -> `XxxMapper.java` trong `mapper/`.
5. Viết Interface nghiệp vụ -> `XxxService.java` trong `service/`.
6. Triển khai logic chi tiết -> `XxxServiceImpl.java` trong `service/impl/`.
7. (Tùy chọn) Nếu nghiệp vụ cần gọi nhiều Service khác -> Viết `XxxFacade.java`.
8. Mở Endpoint nhận Request và trả Response -> `XxxController.java`.