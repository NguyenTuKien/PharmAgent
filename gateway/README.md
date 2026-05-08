# PharmAgent API Gateway

API Gateway được xây dựng bằng **FastAPI**, đóng vai trò là điểm cửa ngõ duy nhất (Single Entry Point) cho toàn bộ hệ thống PharmAgent. Nó chịu trách nhiệm điều hướng traffic, xác thực người dùng, giới hạn lưu lượng (Rate Limiting) và ghi log tập trung.

---

## 📂 Cấu trúc thư mục

```text
gateway/
├── app/
│   ├── middleware/      # Các bộ lọc xử lý request/response toàn cục
│   │   ├── auth.py      # Kiểm tra JWT và phân quyền (Admin, Caregiver, Elderly)
│   │   ├── logging.py   # Ghi log chi tiết mỗi request (method, path, status, time)
│   │   └── rate_limit.py# Giới hạn số lượng request dựa trên Redis
│   ├── routers/         # Định nghĩa các tuyến đường (Routing)
│   │   ├── agent.py     # Điều hướng tới AI Agent Service (HTTP & WebSocket)
│   │   ├── backend.py   # Điều hướng tới Spring Boot Backend (REST & STOMP)
│   │   └── proxy.py     # Logic cốt lõi để chuyển tiếp (forward) request
│   ├── utils/           # Tiện ích dùng chung
│   │   ├── jwt_utils.py # Giải mã và kiểm tra tính hợp lệ của JWT
│   │   └── redis_client.py # Quản lý kết nối tới Redis
│   ├── config.py        # Cấu hình hệ thống (Biến môi trường, URL upstream, secret key)
│   └── main.py          # Điểm khởi chạy ứng dụng, cấu hình Middleware và Routers
├── Dockerfile           # File cấu hình đóng gói Gateway thành Docker Image
└── requirements.txt     # Danh sách các thư viện Python cần thiết (FastAPI, Httpx, Redis...)
```

---

## ⚙️ Vai trò các thành phần chính

1.  **`main.py`**: Trái tim của ứng dụng. Khởi tạo FastAPI, thiết lập thứ tự các Middleware và gộp các Router lại với nhau.
2.  **`middleware/rate_limit.py`**: Sử dụng thuật toán Sliding Window với Redis để ngăn chặn tấn công DOS hoặc spam API. Hỗ trợ các hạn mức khác nhau cho Public, User và Admin.
3.  **`routers/proxy.py`**: Sử dụng thư viện `httpx` (async) để nhận request từ client và "giả dạng" client đó gửi request tới dịch vụ phía sau (Backend/Agent).
4.  **`middleware/auth.py`**: Đọc header `Authorization: Bearer <token>`, giải mã JWT. Nếu hợp lệ, nó sẽ trích xuất thông tin người dùng (`user_id`, `roles`) để các router sử dụng.

---

## 🔄 Luồng hoạt động của một Request

Khi một request từ Client gửi tới Gateway, nó sẽ đi qua các bước sau:

1.  **CORS Check**: Kiểm tra xem domain của client có được phép truy cập không.
2.  **Rate Limiting**: Kiểm tra IP hoặc User đã vượt quá giới hạn request chưa. Nếu vượt quá, trả về `429 Too Many Requests`.
3.  **Access Logging**: Ghi nhận thời điểm bắt đầu request.
4.  **Routing & Security**:
    - Gateway khớp URL (ví dụ: `/ws/agent` sẽ vào `agent.py`, `/api/admin/...` sẽ vào `backend.py`).
    - Nếu route yêu cầu xác thực, `auth.py` sẽ kiểm tra JWT. Nếu không có token hoặc token sai, trả về `401 Unauthorized`.
    - Kiểm tra quyền (Role-based): Nếu user không có quyền `ADMIN` mà truy cập `/api/admin`, trả về `403 Forbidden`.
5.  **Proxying**:
    - Gateway đính kèm thêm các header nội bộ (`X-User-Id`, `X-User-Roles`) vào request.
    - Forward request tới dịch vụ đích (ví dụ: `http://backend:8080/...`).
6.  **Response**:
    - Gateway nhận kết quả từ dịch vụ đích.
    - Trả kết quả về cho Client và ghi log thời gian xử lý (Response Time).

---

## 🚀 Upstream Services

Gateway hiện tại điều hướng tới 2 dịch vụ chính:

| Dịch vụ | URL Nội bộ (Docker) | Prefix tại Gateway | Ghi chú |
| :--- | :--- | :--- | :--- |
| **Backend** | `http://backend:8080` | `/api/**`, `/ws/**` | Spring Boot xử lý logic nghiệp vụ và DB. |
| **AI Agent** | `http://agent:8000` | `/ws/agent`, `/api/agent` | Xử lý nhận diện thuốc real-time qua WebSocket. |

---

## 🛠 Cách chạy (Development)

1.  Cài đặt thư viện: `pip install -r requirements.txt`
2.  Chạy ứng dụng: `uvicorn app.main:app --reload --port 8000`
3.  Tài liệu API (Swagger): Truy cập `http://localhost:8000/docs`
