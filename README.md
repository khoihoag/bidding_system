# Bidding System

Ứng dụng đấu giá thời gian thực viết bằng Java 21. Dự án gồm JavaFX client, TCP server trao đổi dữ liệu JSON, tầng xử lý nghiệp vụ và lưu trữ MySQL thông qua Hibernate.

## 1. Mô tả hệ thống

`Bidding System` là ứng dụng desktop theo mô hình client-server:

- Người dùng thao tác trên giao diện JavaFX.
- Client gửi và nhận JSON qua TCP socket.
- Server xử lý nghiệp vụ qua các controller, service và repository.
- Dữ liệu được lưu vào MySQL bằng Hibernate ORM.

Kiến trúc tổng quát:

```text
JavaFX Client -> TCP/JSON -> Socket Server -> Controllers -> Services -> Repositories -> MySQL
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
| Tiện ích | Lombok |

## 3. Yêu cầu cài đặt

Cài đặt các phần mềm sau:

- JDK 21
- Maven 3.9 trở lên
- MySQL 8 trở lên
- Git nếu cần clone repository

Kiểm tra Java và Maven:

```powershell
java -version
mvn -version
```

## 4. Cấu hình MySQL

Tạo database trước khi chạy server. Nếu máy đã nhận lệnh `mysql`, chạy trực tiếp bằng PowerShell:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Nếu PowerShell báo không tìm thấy `mysql`, tìm vị trí file `mysql.exe`:

```powershell
Get-ChildItem -Path "C:\" -Filter mysql.exe -Recurse -ErrorAction SilentlyContinue
```

Ví dụ máy có `mysql.exe` tại:

```text
C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe
```

Có thể chạy trực tiếp bằng đường dẫn đầy đủ, không cần thêm PATH:

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" --version
```

Hoặc thêm thư mục chứa `mysql.exe` vào User PATH:

```powershell
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";C:\Program Files\MySQL\MySQL Server 9.6\bin",
  "User"
)
```

Lệnh trên chỉ áp dụng cho các terminal mở sau đó. Nếu muốn dùng ngay trong PowerShell hiện tại, chạy thêm:

```powershell
$env:Path += ";C:\Program Files\MySQL\MySQL Server 9.6\bin"
mysql --version
```

Sau đó tạo database:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Nếu không thêm PATH, dùng đường dẫn đầy đủ:

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Cấu hình kết nối database nằm tại:

```text
system/server/src/main/resources/hibernate.cfg.xml
```

Mặc định hiện tại:

```text
URL:      jdbc:mysql://localhost:3306/bidding_system
Username: root
Password: 123456
```

## 5. Cấu trúc thư mục

```text
.
|-- .github/workflows/
|-- pom.xml
|-- README.md
|-- system/
|   |-- client/
|   |   |-- src/main/java/com/bidding/
|   |   `-- src/main/resources/
|   `-- server/
|       |-- src/main/java/com/bidding/server/
|       |-- src/main/resources/
|       `-- src/test/java/com/bidding/server/
`-- target/
```

Các entry point chính:

- Client JavaFX: `com.bidding.App`
- TCP server: `com.bidding.server.network.ServerMain`

## 6. Build và kiểm thử

Chạy từ thư mục gốc của repository:

```powershell
mvn clean test
```

Hoặc chạy verify đầy đủ:

```powershell
mvn clean verify
```

## 7. Chạy ứng dụng

Mở terminal thứ nhất và chạy server:

```powershell
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain"
```

Giữ server đang chạy. Mở terminal thứ hai và chạy client:

```powershell
mvn javafx:run
```

## 8. Dữ liệu demo

Khi database đang trống, server sẽ tạo dữ liệu mẫu tự động. Có thể đăng nhập bằng các tài khoản sau:

| Vai trò | Username | Password |
| --- | --- | --- |
| Admin level 2 | `admin` | `admin123` |
| Người bán | `seller` | `seller123` |
| Người mua | `bidder` | `bidder123` |

## 9. Tạo admin level 2

Nếu pull repo về và chạy với database trống, không cần tạo admin thủ công. Khi server khởi động lần đầu, `DatabaseSeeder` sẽ tự tạo admin level 2:

```text
Username: admin
Password: admin123
```

Lưu ý: seeder chỉ chạy khi bảng người dùng đang trống. Nếu database đã có dữ liệu sẵn, hãy tạo admin level 2 thủ công bằng PowerShell:

```powershell
mysql -u root -p bidding_system -e "INSERT INTO users (user_type, id, createdAt, updatedAt, username, email, passwordHash, fullName, role, isActive, balance, totalRevenue, admin_level) SELECT 'ADMIN', UUID(), NOW(), NOW(), 'admin', 'admin@bidviet.local', 'admin123', 'Quan tri vien', 'ADMIN', 1, 0, 0, 2 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');"
```

Nếu chưa thêm MySQL vào PATH, dùng đường dẫn đầy đủ:

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -u root -p bidding_system -e "INSERT INTO users (user_type, id, createdAt, updatedAt, username, email, passwordHash, fullName, role, isActive, balance, totalRevenue, admin_level) SELECT 'ADMIN', UUID(), NOW(), NOW(), 'admin', 'admin@bidviet.local', 'admin123', 'Quan tri vien', 'ADMIN', 1, 0, 0, 2 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');"
```

Admin level 2 có quyền tạo admin level 1 trong màn quản trị của client.

## 10. Chức năng chính

### Người dùng

- Đăng ký, đăng nhập, cập nhật hồ sơ và nạp tiền vào ví.
- Thêm, sửa, xóa và xem kho vật phẩm cá nhân.
- Gửi vật phẩm cho quản trị viên duyệt trước khi đấu giá.
- Tạo phiên đấu giá ngay hoặc đặt lịch phiên đấu giá.
- Xem danh sách phiên, chi tiết người bán, lịch sử giá và lịch sử đấu giá cá nhân.
- Đặt giá thủ công, cấu hình tự động đặt giá và nhận cập nhật giá theo thời gian thực.
- Theo dõi hoặc bỏ theo dõi phiên đấu giá và nhận thông báo.
- Xem vật phẩm thắng đấu giá và thanh toán.

### Quản trị viên

- Xem danh sách người dùng và số dư.
- Khóa hoặc mở khóa tài khoản người dùng.
- Đóng phiên đấu giá.
- Xem lịch sử đặt giá của phiên đấu giá.
- Xem, duyệt hoặc từ chối vật phẩm đang chờ duyệt.
- Tạo tài khoản quản trị viên level 1.

## 11. Ghi chú phát triển

- Luôn chạy server trước client vì client kết nối đến `localhost:8080`.
- Nếu đổi cổng server, cần cập nhật lại cấu hình trong `NetworkClient`.
- Hibernate đang dùng `hbm2ddl.auto=update`, nên schema có thể được tự cập nhật khi chạy ứng dụng.
- Không commit thông tin database thật hoặc mật khẩu production vào `hibernate.cfg.xml`.
