# Bidding System

## 1. Mô tả bài toán và phạm vi hệ thống

`Bidding System` là ứng dụng đấu giá thời gian thực theo mô hình client-server. Người dùng thao tác trên giao diện JavaFX; client gửi và nhận JSON qua TCP socket với server; server xử lý nghiệp vụ và lưu dữ liệu bằng Hibernate vào MySQL.

Phạm vi hiện tại gồm:

- Quản lý tài khoản người dùng và quản trị viên.
- Quản lý vật phẩm đấu giá thuộc ba nhóm: tranh nghệ thuật, thiết bị điện tử và phương tiện.
- Tạo, đặt lịch, theo dõi và tham gia phiên đấu giá theo thời gian thực.
- Quản lý ví, thanh toán vật phẩm thắng đấu giá và lịch sử giao dịch.
- Quản trị người dùng, phiên đấu giá và duyệt vật phẩm.
- Gọi dịch vụ dự đoán giá AI bên ngoài qua `http://localhost:8000/predict` nếu dịch vụ này đang chạy.

Kiến trúc tổng quát:

```text
JavaFX Client --TCP/JSON--> Socket Server --> Controllers --> Services --> Repositories --> MySQL
```

Server lắng nghe tại `localhost:8080`. Client hiện được cấu hình kết nối tới địa chỉ này.

## 2. Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Giao diện desktop | JavaFX 21, FXML, CSS |
| Build và kiểm thử | Maven, JUnit 5, Mockito |
| Giao tiếp client-server | TCP socket, JSON, Gson |
| Lưu trữ dữ liệu | MySQL, Hibernate ORM |
| Mapping dữ liệu | MapStruct |
| Tích hợp tùy chọn | HTTP client gọi FastAPI dự đoán giá AI |
| CI | GitHub Actions trên Linux, Windows và macOS |

## 3. Yêu cầu cài đặt

Cài đặt các phần mềm sau:

- JDK 21.
- Maven 3.9 trở lên.
- MySQL 8 trở lên.
- Git nếu cần clone repository.
- Dịch vụ FastAPI dự đoán giá AI tại `localhost:8000` nếu muốn dùng chức năng gợi ý giá. Mã nguồn dịch vụ AI không nằm trong repository này.

Kiểm tra Java và Maven:

```bash
java -version
mvn -version
```

Tạo database:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Cấu hình mặc định nằm tại `system/server/src/main/resources/hibernate.cfg.xml`:

```text
URL:      jdbc:mysql://localhost:3306/bidding_system
Username: root
Password: 1234
```

Nên ghi đè thông tin kết nối bằng biến môi trường thay vì sửa file cấu hình.

Linux hoặc macOS (`bash`, `zsh`):

```bash
export DB_URL="jdbc:mysql://localhost:3306/bidding_system"
export DB_USERNAME="root"
export DB_PASSWORD="your_mysql_password"
```

Windows PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/bidding_system"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

Windows Command Prompt:

```bat
set DB_URL=jdbc:mysql://localhost:3306/bidding_system
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password
```

## 4. Cấu trúc thư mục

```text
.
|-- .github/workflows/ci.yml               # CI kiểm thử trên 3 hệ điều hành
|-- pom.xml                                # Cấu hình Maven chung
|-- system/
|   |-- client/
|   |   |-- src/main/java/com/bidding/     # JavaFX client, controller và network client
|   |   `-- src/main/resources/            # FXML, CSS, hình ảnh
|   `-- server/
|       |-- src/main/java/com/bidding/server/
|       |   |-- config/                    # Khởi tạo Hibernate
|       |   |-- controller/                # Xử lý request JSON
|       |   |-- events/                    # Sự kiện đấu giá
|       |   |-- model/                     # Entity và domain model
|       |   |-- network/                   # TCP server và client handler
|       |   |-- repository/                # Truy cập dữ liệu
|       |   |-- service/                   # Nghiệp vụ
|       |   `-- utils/                     # Tiện ích JSON
|       |-- src/main/resources/            # Cấu hình Hibernate
|       `-- src/test/java/                 # Unit test server
`-- target/                                # Kết quả build, được Maven tạo tự động
```

## 5. Build và kiểm thử

Các câu lệnh Maven dưới đây dùng giống nhau trên Linux, macOS, Windows PowerShell và Windows Command Prompt. Chạy từ thư mục gốc của repository.

Biên dịch và chạy toàn bộ unit test:

```bash
mvn clean verify
```

Chỉ chạy unit test:

```bash
mvn test
```

Workflow `.github/workflows/ci.yml` tự động chạy `mvn clean verify` trên:

- `ubuntu-latest`
- `windows-latest`
- `macos-latest`

## 6. Hướng dẫn chạy Server và Client

Thực hiện theo đúng thứ tự sau.

### Bước 1: Khởi động MySQL

Đảm bảo MySQL đang chạy, database `bidding_system` đã tồn tại và thông tin kết nối đã được cấu hình theo mục 3.

### Bước 2: Khởi động server

Mở terminal thứ nhất tại thư mục gốc của repository:

```bash
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain"
```

Khi server chạy thành công, terminal hiển thị thông báo đang lắng nghe tại cổng `8080`.

### Bước 3: Khởi động client

Giữ server đang chạy. Mở terminal thứ hai tại thư mục gốc của repository:

```bash
mvn javafx:run
```

Có thể mở thêm client ở terminal khác để kiểm tra cập nhật đấu giá thời gian thực.

## 7. Chức năng đã hoàn thành

### Người dùng

- Đăng ký, đăng nhập, cập nhật hồ sơ và nạp tiền vào ví.
- Thêm, sửa, xóa và xem kho vật phẩm cá nhân.
- Gửi vật phẩm cho quản trị viên duyệt trước khi đấu giá.
- Tạo phiên đấu giá ngay hoặc đặt lịch phiên đấu giá.
- Xem danh sách phiên, chi tiết người bán, lịch sử giá và lịch sử đấu giá cá nhân.
- Đặt giá thủ công, cấu hình tự động đặt giá và nhận cập nhật giá theo thời gian thực.
- Theo dõi hoặc bỏ theo dõi phiên đấu giá; nhận thông báo.
- Xem vật phẩm thắng đấu giá và thanh toán.
- Yêu cầu dự đoán giá AI khi dịch vụ FastAPI bên ngoài khả dụng.

### Quản trị viên

- Xem danh sách người dùng và số dư.
- Khóa hoặc mở khóa tài khoản người dùng.
- Đóng phiên đấu giá.
- Xem lịch sử đặt giá của phiên đấu giá.
- Xem, duyệt hoặc từ chối vật phẩm đang chờ duyệt.
- Tạo tài khoản quản trị viên cấp 1.

## 8. Báo cáo và video demo

- <a href="https://docs.google.com/document/d/1EG4beJ8RRhDhFjMWVFcCJgh69U4wio0n-QvQpW20QNY/edit?usp=sharing" target="_blank">Báo cáo PDF</a>
- <a href="https://youtu.be/bIVS6nGiM0I?si=pPUAAu9rT_7SRYQ5" target="_blank">Video demo</a>
