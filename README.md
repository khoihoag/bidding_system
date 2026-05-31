# Bidding System

<<<<<<< HEAD
Ứng dụng đấu giá thời gian thực viết bằng Java 21. Dự án gồm JavaFX client, TCP server trao đổi dữ liệu JSON, tầng xử lý nghiệp vụ và lưu trữ MySQL thông qua Hibernate.

## Tính năng chính

- Đăng ký, đăng nhập, cập nhật hồ sơ và nạp tiền tài khoản.
- Quản lý sản phẩm theo người bán, bao gồm thêm, sửa, xóa và duyệt sản phẩm.
- Tạo phiên đấu giá, đặt lịch phiên đấu giá và đóng phiên thủ công.
- Đặt giá, tự động đặt giá, theo dõi phiên đấu giá và xem lịch sử đấu giá.
- Gửi thông báo cho người dùng khi có sự kiện liên quan đến phiên đấu giá.
- Giao diện quản trị để quản lý người dùng, phiên đấu giá, sản phẩm chờ duyệt và lịch sử bid.

## Công nghệ sử dụng

- Java 21
- JavaFX 21
- Maven
- MySQL 8
- Hibernate ORM
- Gson
- JUnit 5 và Mockito
- Lombok, MapStruct

## Cấu trúc dự án

```text
.
├── pom.xml
├── README.md
└── system
    ├── client
    │   └── src/main
    │       ├── java/com/bidding
    │       └── resources
    └── server
        └── src
            ├── main/java/com/bidding/server
            ├── main/resources
            └── test/java/com/bidding/server
```

Các entry point chính:

- Client JavaFX: `com.bidding.App`
- TCP server: `com.bidding.server.network.ServerMain`
- Server lắng nghe tại `localhost:8080`

## Yêu cầu môi trường

- JDK 21
- Maven 3.9 trở lên
- MySQL đang chạy local

Tạo database trước khi chạy server. Nếu máy đã nhận lệnh `mysql`, có thể chạy trực tiếp bằng PowerShell:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Nếu PowerShell báo không tìm thấy `mysql`, tìm vị trí file `mysql.exe` trước:

```powershell
Get-ChildItem -Path "C:\" -Filter mysql.exe -Recurse -ErrorAction SilentlyContinue
```

Sau khi tìm được file, lấy thư mục chứa `mysql.exe` và thêm vào User PATH. Ví dụ nếu `mysql.exe` nằm trong `C:\Program Files\MySQL\MySQL Server 9.6\bin`:

```powershell
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";C:\Program Files\MySQL\MySQL Server 9.6\bin",
  "User"
)
```

Lệnh trên chỉ cập nhật User PATH cho các terminal mở sau đó. Nếu muốn dùng ngay trong PowerShell hiện tại, chạy thêm:

```powershell
$env:Path += ";C:\Program Files\MySQL\MySQL Server 9.6\bin"
```

Hoặc chạy trực tiếp bằng đường dẫn đầy đủ, không cần thêm PATH:

```powershell
& "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" --version
```

Đóng PowerShell hiện tại, mở lại PowerShell mới hoặc dùng `$env:Path` như trên rồi kiểm tra:

```powershell
mysql --version
```

Sau đó chạy lại lệnh tạo database:

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS bidding_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Cấu hình kết nối database tại:

```text
system/server/src/main/resources/hibernate.cfg.xml
```

Mặc định hiện tại:

- URL: `jdbc:mysql://localhost:3306/bidding_system`
- Username: `root`
- Password: `123456`

## Build và test

```bash
mvn clean test
```

## Chạy ứng dụng

Chạy server trước:
=======
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
| CI | GitHub Actions trên Linux, Windows và macOS |

## 3. Yêu cầu cài đặt

Cài đặt các phần mềm sau:

- JDK 21.
- Maven 3.9 trở lên.
- MySQL 8 trở lên.
- Git nếu cần clone repository.

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
>>>>>>> 633fc6d0480dd215738e884d479b2de7bfd24793

```bash
mvn exec:java -Dexec.mainClass="com.bidding.server.network.ServerMain"
```

<<<<<<< HEAD
Sau đó mở một terminal khác và chạy client:
=======
Khi server chạy thành công, terminal hiển thị thông báo đang lắng nghe tại cổng `8080`.

### Bước 3: Khởi động client

Giữ server đang chạy. Mở terminal thứ hai tại thư mục gốc của repository:
>>>>>>> 633fc6d0480dd215738e884d479b2de7bfd24793

```bash
mvn javafx:run
```

<<<<<<< HEAD
## Dữ liệu demo

Khi database đang trống, server sẽ tạo dữ liệu mẫu tự động. Có thể đăng nhập bằng các tài khoản sau:

| Vai trò | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `admin123` |
| Người bán | `seller` | `seller123` |
| Người mua | `bidder` | `bidder123` |

## Tạo admin level 2

Nếu pull repo về và chạy với database trống, không cần tạo thủ công. Khi server khởi động lần đầu, `DatabaseSeeder` sẽ tự tạo admin level 2:

- Username: `admin`
- Password: `admin123`

Lưu ý: seeder chỉ chạy khi bảng người dùng đang trống. Nếu database đã có dữ liệu sẵn, hãy tạo admin level 2 thủ công bằng PowerShell:

```powershell
mysql -u root -p bidding_system -e "INSERT INTO users (user_type, id, createdAt, updatedAt, username, email, passwordHash, fullName, role, isActive, balance, totalRevenue, admin_level) SELECT 'ADMIN', UUID(), NOW(), NOW(), 'admin', 'admin@bidviet.local', 'admin123', 'Quan tri vien', 'ADMIN', 1, 0, 0, 2 WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');"
```

Sau khi tạo xong, đăng nhập bằng:

```text
Username: admin
Password: admin123
```

Admin level 2 có quyền tạo admin level 1 trong màn quản trị của client.

## Kiến trúc xử lý

```text
JavaFX Client
    -> NetworkClient
    -> TCP/JSON
    -> ServerMain / ClientHandler
    -> Controllers
    -> Services
    -> Repositories
    -> Hibernate
    -> MySQL
```

Client gửi request JSON có trường `action`; server định tuyến request trong `ClientHandler` đến controller tương ứng như `AuthController`, `ItemController`, `AuctionController` hoặc `AdminController`.

## Ghi chú phát triển

- Luôn chạy server trước client vì client kết nối đến `localhost:8080`.
- Nếu đổi cổng server, cần cập nhật lại cấu hình trong `NetworkClient`.
- Hibernate đang dùng `hbm2ddl.auto=update`, nên schema có thể được tự cập nhật khi chạy ứng dụng.
- Không commit thông tin database thật hoặc mật khẩu production vào `hibernate.cfg.xml`.
=======
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
- Chức năng nâng cao : Auto-Bidding, Anti-Sniping , Bid History Visualization
- Chức năng tự sáng tạo : Follow phiên đấu giá ( sẽ nhân được thông báo khi giá thay đổi và khi phiên kết thúc ) , Đấu giá ngược ( giá tự động giảm dần theo thời gian người đấu giá đầu tiên sẽ thắng)
  
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
>>>>>>> 633fc6d0480dd215738e884d479b2de7bfd24793

