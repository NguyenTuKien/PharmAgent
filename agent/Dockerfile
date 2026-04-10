# 1. Sử dụng Python bản slim để dung lượng image nhẹ nhất
FROM python:3.10-slim

# 2. Thiết lập thư mục làm việc trong container
WORKDIR /app

# 3. Thiết lập biến môi trường
ENV PYTHONDONTWRITEBYTECODE 1
ENV PYTHONUNBUFFERED 1

# 4. Cài đặt các thư viện hệ thống cần thiết
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 5. Tạo user không có quyền root để bảo mật
RUN adduser --disabled-password --gecos "" appuser && chown -R appuser /app

# 6. Copy file requirements và cài đặt dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 7. Copy toàn bộ mã nguồn vào container và đổi quyền sở hữu
COPY . .
RUN chown -R appuser /app

# 8. Chuyển sang user không có quyền root
USER appuser

# 9. Mở cổng 8000
EXPOSE 8000

# 10. Lệnh khởi chạy ứng dụng sử dụng Uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]