# Cinema Booking REST API

Backend đặt vé xem phim, hỗ trợ quản lý phim, phòng chiếu, suất chiếu và đặt nhiều ghế trong một giao dịch. Trọng tâm của dự án là xử lý quy tắc lịch chiếu và chống đặt trùng ghế khi có yêu cầu đồng thời.

Ứng dụng chạy dưới dạng một Spring Boot service, tổ chức mã nguồn theo miền nghiệp vụ: `auth`, `movie`, `cinema`, `showtime`, `booking` và `common`.

## Chức năng chính

- Đăng ký, đăng nhập bằng JWT; phân quyền `ADMIN` và `CUSTOMER`.
- Quản lý phim; danh sách phim công khai gồm `NOW_SHOWING` và `COMING_SOON`.
- Tạo phòng và tự sinh ghế theo hàng A–Z, ví dụ A1, A2, B1.
- Tạo suất chiếu, tính giờ kết thúc từ thời lượng phim và kiểm tra khoảng nghỉ tối thiểu 15 phút giữa các suất trong cùng phòng.
- Tra cứu suất chiếu theo phim, ngày và xem trạng thái ghế.
- Đặt nhiều ghế, tính tiền ở server, xem vé của chính mình và hủy vé trước giờ chiếu để giải phóng ghế.

Hiện tại đặt vé thành công sẽ chuyển thẳng sang `CONFIRMED`. Dự án chưa tích hợp thanh toán, giữ ghế có thời hạn hoặc hoàn tiền. Hủy suất chiếu chỉ đổi trạng thái suất sang `CANCELLED`, chưa tự động hủy các booking liên quan.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ, framework | Java 21, Spring Boot 3.4.3 |
| HTTP API | Spring Web, Bean Validation |
| Bảo mật | Spring Security, JJWT 0.12.6, BCrypt |
| Dữ liệu | Spring Data JPA, MySQL 8.0, Flyway |
| Tài liệu API | Springdoc OpenAPI 2.8.5, Swagger UI |
| Kiểm thử | JUnit 5, Mockito, MockMvc, Testcontainers MySQL |
| Đóng gói | Maven Wrapper, Docker multi-stage build, Docker Compose |

## Khởi chạy bằng Docker Compose

Cần Docker cùng Docker Compose v2; trên Windows/macOS có thể dùng Docker Desktop. Chạy các lệnh từ thư mục gốc dự án.

**1. Tạo file môi trường**

PowerShell:

```powershell
Copy-Item .env.example .env
```

Bash:

```bash
cp .env.example .env
```

**2. Build và chạy API cùng MySQL**

```sh
docker compose up -d --build
docker compose logs -f api-service
```

Compose chờ MySQL vượt qua healthcheck rồi mới khởi động API. Flyway tạo schema và chạy các migration khi ứng dụng khởi động; chờ log báo ứng dụng đã chạy trước khi gọi API.

Với cổng mặc định:

| Địa chỉ | Mục đích |
| --- | --- |
| [Swagger UI](http://localhost:8080/swagger-ui.html) | Xem và thử API |
| [OpenAPI JSON](http://localhost:8080/api-docs) | Đặc tả API |
| [Danh sách phim](http://localhost:8080/api/movies) | Kiểm tra API công khai; ban đầu có thể trả `[]` |

Schema mới chỉ có tài khoản admin từ migration V5, chưa có phim, phòng hay suất chiếu mẫu. Xem mục **Tài khoản và luồng chạy thử** để tạo dữ liệu.

Kiểm tra container hoặc dừng dịch vụ:

```sh
docker compose ps
docker compose down
```

Dữ liệu MySQL nằm trong named volume và được giữ lại sau `docker compose down`.

## Cấu hình môi trường

Các biến sau trong `.env` được `compose.yaml` sử dụng:

| Biến | Mặc định trong `.env.example` | Ý nghĩa |
| --- | --- | --- |
| `APP_PORT` | `8080` | Cổng API trên máy host |
| `DB_PORT` | `3306` | Cổng MySQL trên máy host |
| `DB_NAME` | `cinema_db` | Tên database |
| `DB_USER` | `cinema_user` | User database của ứng dụng |
| `DB_PASSWORD` | `cinema_password` | Mật khẩu user database |
| `DB_ROOT_PASSWORD` | `rootpassword` | Mật khẩu root MySQL |
| `JWT_SECRET` | Chuỗi mẫu cần thay | Khóa ký JWT; tạo riêng cho mỗi môi trường, tối thiểu 32 byte UTF-8 |

Khi đổi `APP_PORT`, thay cổng trong các URL ở trên. API trong container luôn kết nối MySQL qua `mysql-db:3306`; `DB_PORT` chỉ đổi cổng truy cập từ máy host.

`.env` phục vụ Docker Compose; Spring Boot chạy trực tiếp bằng IDE/Maven không tự nạp file này. File `.env` đã được bỏ qua trong `.gitignore`; `.env.example` cung cấp cấu hình mẫu để tạo file môi trường trên mỗi máy.

Các giá trị mẫu dành cho phát triển. Thay `JWT_SECRET` trong `.env` bằng khóa ngẫu nhiên riêng trước khi chạy. Compose yêu cầu `DB_PASSWORD`, `DB_ROOT_PASSWORD` và `JWT_SECRET` có giá trị; thiếu hoặc rỗng sẽ báo lỗi khi đọc cấu hình. Với database đã có dữ liệu trong volume, thay biến mật khẩu không tự đổi mật khẩu trong MySQL.

Khi không chọn profile, ứng dụng dùng `dev`, gồm SQL logging và khóa JWT dự phòng dành cho local. Compose chọn rõ profile `docker`, tắt SQL logging và lấy khóa JWT từ môi trường; thời hạn JWT là 24 giờ. Trước khi triển khai thực tế, cần thay mật khẩu mẫu và xử lý tài khoản admin mẫu.

Dockerfile build JAR bằng Maven/JDK ở stage đầu, rồi chạy bằng JRE với user `appuser` ở stage cuối. File `.dockerignore` loại `.env`, kết quả build local và cấu hình IDE khỏi build context. Có thể kiểm tra cấu hình trước khi chạy bằng `docker compose config --quiet`.

## Chạy local bằng Maven hoặc IDE

Cần JDK 21 và Docker để chạy MySQL. Dự án có Maven Wrapper nên không bắt buộc cài Maven riêng.

**1. Chỉ khởi động MySQL dành cho phát triển**

```sh
docker compose -f docker-compose.dev.yml up -d
docker compose -f docker-compose.dev.yml logs -f mysql-dev
```

Chờ MySQL sẵn sàng nhận kết nối. File này dùng cố định cổng `3306` và tài khoản `cinema_user` / `cinema_password`, database `cinema_db`, khớp với `application-dev.properties`.

**2. Chạy ứng dụng**

PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Bash:

```bash
./mvnw spring-boot:run
```

Hoặc chạy `CinemaBookingApiApplication` trong IDE với JDK 21. Các URL truy cập giống cách chạy Docker ở trên.

Nếu dùng database khác, đặt `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` trong môi trường chạy ứng dụng. Có thể đặt thêm `JWT_SECRET` và `SERVER_PORT` khi chạy local.

Hai file Compose dùng database volume riêng nhưng cùng chiếm cổng host `3306` theo mặc định. Hãy dừng bộ đang chạy trước khi chuyển cách chạy; dữ liệu không tự đồng bộ giữa hai bộ.

## Tài khoản và luồng chạy thử

### Xác thực

`POST /api/auth/register` luôn tạo tài khoản `CUSTOMER` và trả JWT ngay khi đăng ký thành công. Request mẫu:

```json
{
  "email": "customer@example.com",
  "password": "Customer@123",
  "fullName": "Khách hàng Demo"
}
```

`POST /api/auth/login` nhận `email` và `password`. Lấy `accessToken` từ response và gửi vào các API cần xác thực:

```http
Authorization: Bearer <accessToken>
```

Trong Swagger UI, bấm **Authorize** và dán riêng giá trị token. Theo cấu hình hiện tại, API đặt vé yêu cầu role `CUSTOMER`; tài khoản `ADMIN` dùng cho nhóm `/api/admin/**`.

### Tài khoản admin mẫu

Migration `V5__seed_default_admin.sql` tạo sẵn tài khoản quản trị trên database mới:

| Email | Mật khẩu | Role |
| --- | --- | --- |
| `admin@cinema.com` | `Admin@123` | `ADMIN` |

Gọi `POST /api/auth/login` bằng thông tin này để lấy token quản trị. Mật khẩu được lưu dưới dạng BCrypt hash; không cần tạo lại hash mỗi lần đăng nhập.

Nếu database đã chạy bản V5 cũ, sửa file SQL không tự cập nhật tài khoản và có thể gây lỗi checksum Flyway. Database đó cần được xử lý riêng trước khi dùng bản V5 đã sửa; không xóa dữ liệu đang cần chỉ để chạy lại migration.

### Thử một luồng đặt vé

1. Dùng token admin tạo phòng, tạo phim rồi tạo suất chiếu trong tương lai. Lấy các ID thực tế từ response.
2. Gọi `GET /api/showtimes/{id}/seats`, chọn ghế có trạng thái `AVAILABLE`.
3. Dùng token customer gọi `POST /api/bookings` với body:

   ```json
   {
     "showtimeId": 1,
     "seatIds": [1, 2]
   }
   ```

   Thay ID mẫu bằng dữ liệu thực tế. `seatIds` là trường **`seatId`** trong response sơ đồ ghế, không phải trường `id` của bản ghi `ShowtimeSeat`. Server tự tính tổng tiền theo giá suất chiếu nhân số ghế.

4. Đặt lại các ghế đó để kiểm tra lỗi `409 SEAT_ALREADY_BOOKED`.
5. Xem vé qua `GET /api/bookings/me`, hủy bằng `PATCH /api/bookings/{id}/cancel` trước giờ chiếu, rồi kiểm tra ghế trở về `AVAILABLE`.

### Postman

Import [collection có sẵn](postman/cinema%20booking%20api.postman_collection.json), đặt collection variables `baseUrl` thành `http://localhost:8080` và `showDate` thành một ngày tương lai theo định dạng `YYYY-MM-DD`.

Chạy lần lượt các folder Auth → Admin Management → Public Discovery → Customer Booking. Request **Login Admin** đã dùng tài khoản mẫu ở trên. Các script lưu token và ID vào collection variables. Khi chạy lại, đổi email đăng ký và tên phòng nếu dữ liệu đó đã tồn tại.

## API chính

| Quyền truy cập | Method | Endpoint | Chức năng |
| --- | --- | --- | --- |
| Công khai | `POST` | `/api/auth/register` | Đăng ký customer |
| Công khai | `POST` | `/api/auth/login` | Đăng nhập |
| Công khai | `GET` | `/api/movies` | Danh sách phim đang/sắp chiếu |
| Công khai | `GET` | `/api/movies/{id}` | Chi tiết phim |
| Công khai | `GET` | `/api/showtimes?movieId=...&date=YYYY-MM-DD` | Tra cứu suất chiếu |
| Công khai | `GET` | `/api/showtimes/{id}/seats` | Sơ đồ, trạng thái ghế |
| `ADMIN` | `GET`, `POST` | `/api/admin/movies` | Danh sách, tạo phim |
| `ADMIN` | `PUT` | `/api/admin/movies/{id}` | Cập nhật phim |
| `ADMIN` | `PATCH` | `/api/admin/movies/{id}/status` | Đổi trạng thái phim |
| `ADMIN` | `GET`, `POST` | `/api/admin/auditoriums` | Danh sách, tạo phòng |
| `ADMIN` | `GET`, `PUT` | `/api/admin/auditoriums/{id}` | Chi tiết, đổi tên phòng |
| `ADMIN` | `GET` | `/api/admin/auditoriums/{id}/seats` | Ghế của phòng |
| `ADMIN` | `GET`, `POST` | `/api/admin/showtimes` | Danh sách, tạo suất chiếu |
| `ADMIN` | `GET` | `/api/admin/showtimes/{id}` | Chi tiết suất chiếu |
| `ADMIN` | `PATCH` | `/api/admin/showtimes/{id}/cancel` | Hủy suất chiếu |
| `CUSTOMER` | `POST` | `/api/bookings` | Đặt vé |
| `CUSTOMER` | `GET` | `/api/bookings/me` | Danh sách vé của mình |
| `CUSTOMER` | `GET` | `/api/bookings/{id}` | Chi tiết vé của mình |
| `CUSTOMER` | `PATCH` | `/api/bookings/{id}/cancel` | Hủy vé của mình |

Hai tham số `movieId`, `date` của API tra cứu suất chiếu đều tùy chọn. Nếu không truyền ngày, API lấy ngày hiện tại tại `Asia/Ho_Chi_Minh` và chỉ trả suất `SCHEDULED`. Trường `startTime` khi tạo suất dùng định dạng `YYYY-MM-DDTHH:mm:ss` và phải ở tương lai.

API cập nhật phòng chỉ hỗ trợ đổi tên và nhận trường `name` (không được để trống, tối đa 100 ký tự). Kích thước phòng và sơ đồ ghế được xác định khi tạo phòng. Chi tiết request/response xem tại Swagger UI.

## Giao dịch và chống đặt trùng

- Tạo suất chiếu khóa phòng bằng `PESSIMISTIC_WRITE` trước khi kiểm tra trùng lịch và tạo kho ghế riêng cho suất.
- Đặt vé chạy trong một transaction: khóa suất chiếu, khóa các ghế theo thứ tự `seatId` tăng dần, kiểm tra ghế rồi tạo booking và cập nhật trạng thái ghế.
- Hủy vé lấy ID suất chiếu của booking thuộc khách hàng hiện tại, sau đó khóa suất chiếu, đọc và khóa booking, kiểm tra điều kiện hủy rồi khóa các ghế theo thứ tự `seatId` tăng dần trước khi giải phóng. Booking đã hủy bị từ chối trước khi cập nhật ghế.
- Admin hủy suất chiếu cũng khóa suất trước khi đổi trạng thái sang `CANCELLED`. Yêu cầu đặt vé lấy được khóa sau khi thao tác hủy suất đã commit sẽ bị từ chối; các booking đã tạo trước đó chưa được tự động hủy.
- Nếu bất kỳ ghế nào không hợp lệ hoặc đã được đặt, toàn bộ giao dịch đặt vé bị rollback.
- Khóa ở mức suất chiếu khiến các giao dịch đặt vé cùng suất được xử lý tuần tự, kể cả khi chọn ghế khác nhau.

Các lỗi được xử lý tập trung qua `GlobalExceptionHandler`, gồm mã HTTP, mã nghiệp vụ và thông báo. Ví dụ khi đặt ghế đã được đặt:

```json
{
  "timestamp": "2026-10-19T00:00:00Z",
  "status": 409,
  "code": "SEAT_ALREADY_BOOKED",
  "message": "Ít nhất một ghế đã được người khác đặt trước",
  "path": "/api/bookings"
}
```

Lỗi Bean Validation có thêm `fieldErrors`. Các trường hợp thường gặp gồm `401` khi xác thực thất bại, `403` khi thiếu quyền, `404` khi không tìm thấy tài nguyên và `409` khi xung đột nghiệp vụ.

## Kiểm thử

PowerShell:

```powershell
# Unit test và MockMvc, không cần MySQL hoặc Docker
.\mvnw.cmd "-Dtest=*ServiceTest,AuthControllerTest" test

# Integration test: tranh ghế, hủy booking và hủy suất đồng thời
.\mvnw.cmd "-Dtest=BookingConcurrencyIntegrationTest" test

# Toàn bộ test
.\mvnw.cmd test
```

Với Bash, thay `.\mvnw.cmd` bằng `./mvnw`.

`BookingConcurrencyIntegrationTest` cần Docker đang chạy và dùng MySQL container riêng qua Testcontainers. Test đặt vé kiểm tra 2, 10 và 50 khách khác nhau tranh cùng một ghế chỉ tạo một booking; lỗi ngoài xung đột ghế được đưa về luồng test qua `Future.get()`. Test hủy vé kiểm tra hai yêu cầu hủy cùng booking chỉ thành công một lần; sau khi người khác đặt lại ghế, thử hủy booking cũ bị từ chối và ghế vẫn thuộc booking mới.

Hai test đặt vé đồng thời với hủy suất chủ động giữ transaction đầu tiên chưa commit để kiểm tra thao tác thứ hai phải chờ: admin hủy trước thì đặt vé bị từ chối; đặt vé trước thì booking hoàn tất trước khi suất bị hủy. Theo nghiệp vụ hiện tại, booking có trước vẫn được giữ. Các test này chưa triển khai thanh toán. Lần đầu chạy cần tải image `mysql:8.0`.

`CinemaBookingApiApplicationTests` cũng import cấu hình Testcontainers và dùng MySQL container riêng, nên không cần MySQL local để chạy các test này; Docker phải đang chạy. Dockerfile dùng `-DskipTests` khi build image; build thành công chưa xác nhận các test đã chạy.

## Cấu trúc mã nguồn

```text
src/main/java/com/cinema/booking/
├── auth/       # Đăng ký, đăng nhập, JWT, Spring Security
├── movie/      # Phim và trạng thái chiếu
├── cinema/     # Phòng chiếu và ghế vật lý
├── showtime/   # Lịch chiếu và trạng thái ghế theo suất
├── booking/    # Giao dịch đặt vé, xem vé, hủy vé
└── common/     # Exception, response lỗi, cấu hình OpenAPI

src/main/resources/
├── application.properties
├── application-dev.properties
├── application-docker.properties
└── db/migration/  # V1–V5: schema và tài khoản admin mẫu

src/test/         # Unit, MockMvc, context và concurrency test
postman/          # Collection chạy thử API
compose.yaml      # API + MySQL
docker-compose.dev.yml  # Chỉ MySQL cho phát triển local
```
