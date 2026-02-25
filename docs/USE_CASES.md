# Tài liệu Đặc tả Use Case (USE CASES)

Tài liệu này mô tả các ca sử dụng (Use Cases) của hệ thống Quản lý Sinh viên.

---

## UC-11: Quản lý Người dùng (User Management)

_(Đã được triển khai trong các module Auth/User)_

---

## UC-13: Quản lý Khoa (Department Management)

_(Đã được triển khai trong AdminDepartmentController)_

---

## UC-14: Quản lý Lớp học (Class Management)

### 1. Mô tả (Description)

Cho phép Quản trị viên (Admin) quản lý các lớp học được mở trong từng học kỳ. Admin có thể tạo lớp mới dựa trên môn học có sẵn, phân công giảng viên, xếp lịch học, phòng học và quản lý sĩ số sinh viên.

### 2. Tác nhân (Actors)

- Admin (Quản trị viên)

### 3. Các luồng công việc (Flows)

#### UC-14.1: Xem danh sách Lớp học

- **Luồng chính**:
  1. Admin chọn menu "Class Management".
  2. Hệ thống tải danh sách các lớp học hiện có.
  3. Hiển thị: Mã lớp, Tên môn, Giảng viên, Học kỳ, Lịch học, Phòng học, Sĩ số (Số lượng hiện tại / Tối đa), Trạng thái.
  4. Admin có thể tìm kiếm theo tên môn/giảng viên và lọc theo Học kỳ, Khoa, Trạng thái.

#### UC-14.2: Thêm Lớp học mới (Create Class)

- **Luồng chính**:
  1. Admin nhấn "Add New Class".
  2. Admin chọn Môn học (Course), Giảng viên (Teacher), và Học kỳ (Semester) từ các danh sách có sẵn.
  3. Admin nhập thông tin: Phòng học (Room), Lịch học (Schedule - ví dụ: Mon 08:00-10:00), Sĩ số tối đa.
  4. Admin nhấn "Create Class".
  5. Hệ thống kiểm tra tính hợp lệ (các trường bắt buộc) và lưu thông tin lớp học.
  6. Trạng thái mặc định của lớp mới là `OPEN`.

#### UC-14.3: Chỉnh sửa Lớp học (Edit Class)

- **Luồng chính**:
  1. Admin nhấn biểu tượng "Edit" trên dòng lớp học tương ứng.
  2. Hệ thống hiển thị modal chứa thông tin hiện tại của lớp.
  3. Admin cập nhật các thông tin mong muốn (Môn học, Giảng viên, Lịch học, Phòng, Sĩ số...).
  4. Admin nhấn "Save Changes".
  5. Hệ thống cập nhật dữ liệu và hiển thị thông báo thành công.

#### UC-14.4: Xem chi tiết và Danh sách Sinh viên

- **Luồng chính**:
  1. Admin nhấn vào icon "View Students" hoặc click vào mã lớp.
  2. Hệ thống hiển thị trang Chi tiết Lớp học gồm: Thông tin tổng quan lớp và bảng Danh sách sinh viên đã đăng ký.
  3. Admin có thể tìm kiếm sinh viên trong danh sách hoặc thực hiện gỡ sinh viên khỏi lớp.

#### UC-14.5: Cập nhật Trạng thái Lớp học

- **Luồng chính**:
  1. Admin có thể thay đổi trạng thái lớp học.
  2. Các trạng thái gồm:
     - `OPEN`: Đang mở đăng ký.
     - `CLOSED`: Khóa đăng ký (khi đủ sĩ số hoặc Admin chủ động đóng).
     - `CANCELLED`: Lớp bị hủy.

### 4. Các ràng buộc và Quy tắc (Constraints/Business Rules)

- Một lớp học phải luôn gắn liền với một Môn học và một Học kỳ.
- Khi một lớp ở trạng thái `CANCELLED`, sinh viên không thể đăng ký vào lớp này.
- Sĩ số sinh viên (`students`) không được vượt quá sĩ số tối đa (`capacity`) khi sinh viên tự đăng ký (Admin có thể override nếu cần).
- Soft Delete: Khi xóa lớp học, hệ thống sẽ ẩn lớp đó khỏi danh sách hiển thị nhưng vẫn giữ lại thông tin lịch sử trong cơ sở dữ liệu nếu đã có sinh viên đăng ký.

---

---

## UC-15: Bảng điều khiển Sinh viên (Student Dashboard)

### 1. Mô tả

Cung cấp cái nhìn tổng quan về tình hình học tập và các hoạt động sắp tới của sinh viên.

### 2. Tác nhân

- Sinh viên (Student)

### 3. Thông tin hiển thị

- **Số liệu thống kê**: GPA hiện tại, Tổng số tín chỉ đã tích lũy, Tỷ lệ chuyên cần.
- **Lịch học hôm nay**: Danh sách các tiết học trong ngày hiện tại (Thời gian, Môn, Phòng).
- **Hạn chót sắp tới**: Các bài tập hoặc thông báo cần chú ý.
- **Tiến độ tốt nghiệp**: Biểu đồ phần trăm hoàn thành chương trình học.

---

## UC-16: Đăng ký Học phần (Course Registration)

### 1. Mô tả

Cho phép sinh viên chủ động đăng ký các lớp học được mở trong học kỳ hiện tại.

### 2. Tác nhân

- Sinh viên (Student)

### 3. Các luồng công việc

#### UC-16.1: Xem danh sách lớp có thể đăng ký

- Hệ thống lọc danh sách lớp dựa trên Khoa của sinh viên và trạng thái lớp (`OPEN`).
- Hiển thị: Tên môn, Số tín chỉ, Giảng viên, Lịch học, Sĩ số còn lại.

#### UC-16.2: Thực hiện đăng ký

- Sinh viên nhấn "Enroll" cho lớp mong muốn.
- Hệ thống kiểm tra:
  - Sinh viên đã đăng ký môn này chưa?
  - Lớp còn chỗ không?
  - Có bị trùng lịch với các lớp đã đăng ký không?
- Thông báo kết quả đăng ký thành công hoặc lý do thất bại.

#### UC-16.3: Hủy đăng ký (Drop Class)

- Sinh viên có thể rút khỏi lớp đã đăng ký nếu vẫn trong thời hạn cho phép.

---

## UC-17: Kết quả Học tập (Academic Records)

### 1. Mô tả

Xem điểm số và quá trình tích lũy học tập của sinh viên.

### 2. Tác nhân

- Sinh viên (Student)

### 3. Các luồng công việc

- **Xem điểm theo kỳ**: Hiển thị bảng điểm chi tiết của từng môn trong học kỳ được chọn.
- **Xem bảng điểm tổng hợp (Transcript)**: Xem toàn bộ quá trình học từ lúc nhập học.

---

## UC-18: Lịch học Cá nhân (Student Schedule)

### 1. Mô tả

Cung cấp lịch học chi tiết cho toàn bộ học kỳ.

### 2. Tác nhân

- Sinh viên (Student)

### 3. Tính năng

- Chế độ xem theo Tuần hoặc theo Tháng.
- Hiển thị đầy đủ thông tin lớp, phòng học và giảng viên.

---

## UC-19: Quản lý Thông tin cá nhân (Profile Management)

### 1. Mô tả

Sinh viên tự cập nhật thông tin liên lạc và ảnh đại diện.

### 2. Tác nhân

- Sinh viên (Student)

### 3. Tính năng

- Thay đổi số điện thoại, địa chỉ thường trú.
- Tải lên ảnh đại diện cá nhân.

---

## UC-20: Hệ thống Thông báo (Notification System)

### 1. Mô tả (Description)

Cung cấp khả năng gửi và nhận thông báo thời gian thực giúp cải thiện tương tác giữa Nhà trường (Admin), Giảng viên (Teacher) và Sinh viên (Student). Hệ thống sử dụng Firebase Cloud Messaging (FCM) để đẩy thông báo ngay lập tức.

### 2. Tác nhân (Actors)

- **Admin**: Người gửi thông báo chung toàn trường hoặc theo nhóm.
- **Teacher**: Người kích hoạt các thông báo liên quan đến lớp học và điểm số.
- **Student**: Người nhận thông báo.
- **Hệ thống (System)**: Tự động gửi các thông báo nhắc lịch, cập nhật hệ thống.

> **Xem chi tiết tài liệu thiết kế dành riêng cho Admin tại: [ADMIN_NOTIFICATION_MANAGEMENT_UC.md](./ADMIN_NOTIFICATION_MANAGEMENT_UC.md)**

---

### 3. Các luồng công việc chi tiết (Detailed Flows)

#### UC-20.1: Đăng ký thiết bị và Token (Device & Token Registration)

- **Mục tiêu**: Liên kết trình duyệt của người dùng với hệ thống thông báo.
- **Luồng chính**:
  1. Người dùng đăng nhập vào Portal (hoặc Admin Dashboard).
  2. Frontend kiểm tra quyền thông báo của trình duyệt:
     - Nếu chưa hỏi: Hiển thị popup yêu cầu quyền.
     - Nếu bị chặn: Hiển thị hướng dẫn mở quyền trong phần cài đặt.
  3. Trình duyệt cấp quyền -> Frontend lấy **FCM Token** từ Firebase.
  4. Frontend gọi API `POST /notifications/tokens` gửi Token và loại thiết bị (`web`).
  5. Backend kiểm tra:
     - Nếu Token đã tồn tại: Cập nhật User ID và thời gian cập nhật mới nhất.
     - Nếu Token chưa có: Tạo mới bản ghi trong bảng `fcm_tokens`.
- **Kết quả**: Hệ thống biết gửi thông báo đến máy tính/trình duyệt nào khi cần liên hệ với User.

#### UC-20.2: [Admin] Gửi thông báo diện rộng (Global Broadcast)

- **Mục tiêu**: Admin gửi thông tin quan trọng cho toàn bộ thành viên hệ thống (ví dụ: Thông báo nghỉ lễ, bảo trì).
- **Luồng chính**:
  1. Admin vào menu **System Management** -> **Notifications**.
  2. Admin nhấn **"Create Broadcast"**.
  3. Admin nhập:
     - **Tiêu đề**: Ví dụ "Thông báo nghỉ Tết Nguyên Đán".
     - **Nội dung**: Chi tiết lịch nghỉ và ngày quay lại.
     - **Action URL**: Link đến file văn bản/quyết định (tùy chọn).
  4. Admin nhấn **"Send Notification"**.
  5. **Hệ thống xử lý**:
     - Lưu dữ liệu thông báo vào bảng `notifications`.
     - Truy vấn tất cả Token đang hoạt động trong bảng `fcm_tokens`.
     - Gửi yêu cầu đẩy tin nhắn hàng loạt đến Firebase API.
- **Kết quả**: Tất cả người dùng đang online (hoặc có đăng ký token) sẽ nhận được popup thông báo.

#### UC-20.3: [Teacher/Admin] Thông báo theo Lớp/Môn học (Targeted Class Notification)

- **Mục tiêu**: Giảng viên hoặc Admin thông báo riêng cho sinh viên của một lớp cụ thể (ví dụ: Dời lịch học, Hủy lớp).
- **Luồng chính**:
  1. Giảng viên vào trang **Class Management** -> Chọn một lớp đang giảng dạy.
  2. Nhấn nút **"Notify Class"**.
  3. Nhập nội dung: "Lớp chiều nay nghỉ do giảng viên họp đột xuất, lịch bù sẽ thông báo sau".
  4. Nhấn **"Submit"**.
  5. **Hệ thống xử lý**:
     - Xác định danh sách Sinh viên đang đăng ký lớp này.
     - Với mỗi Sinh viên:
       - Lưu một bản ghi thông báo cá nhân vào DB.
       - Tìm các thiết bị (tokens) của sinh viên đó và gửi qua FCM.
- **Kết quả**: Chỉ các sinh viên trong lớp đó nhận được thông báo.

#### UC-20.4: [System] Thông báo kết quả học tập (Academic Notifications)

- **Mục tiêu**: Tự động báo cho sinh viên khi có điểm mới.
- **Luồng chính**:
  1. Giảng viên hoàn tất nhập điểm và nhấn **"Publish Grades"**.
  2. Hệ thống (Backend) thực hiện cập nhật trạng thái điểm.
  3. Sau khi lưu thành công, hệ thống tự động gọi `NotificationService`:
     - Tạo thông báo: "Môn [Tên Môn] đã có điểm chính thức. Click để xem chi tiết."
     - Gửi thông báo đến trang cá nhân của từng sinh viên tương ứng qua FCM.
- **Kết quả**: Sinh viên biết ngay khi có điểm mà không cần vào kiểm tra thủ công.

#### UC-20.5: [Người dùng] Xem và Quản lý thông báo (View & Manage Notifications)

- **Luồng chính**:
  1. Người dùng thấy icon hình quả chuông có số lượng thông báo chưa đọc.
  2. Người dùng nhấn vào icon để mở danh sách nhanh (Dropdown).
  3. Nhấn vào một thông báo cụ thể:
     - Chuyển hướng đến **Action URL** (nếu có).
     - Gọi API `PATCH /notifications/{id}/read` để đánh dấu đã đọc.
     - Giảm số lượng thông báo chưa đọc trên icon.
  4. Người dùng có thể nhấn **"View All"** để vào trang lịch sử thông báo đầy đủ.

---

### 4. Các Quy tắc nghiệp vụ (Business Rules)

1. **Đa thiết bị**: Một người dùng nhận được thông báo trên tất cả các trình duyệt đã dùng để đăng nhập (Chrome trên PC, Safari trên Mobile...).
2. **Offline**: Nếu người dùng không mở trình duyệt, thông báo sẽ được lưu trong DB và hiển thị khi họ quay lại. Nếu họ dùng trình duyệt hỗ trợ Service Worker, họ vẫn nhận được thông báo hệ thống ngay cả khi không mở tab ứng dụng.
3. **Phân quyền**:
   - Teacher chỉ được gửi thông báo cho các lớp mình phụ trách.
   - Admin được quyền gửi cho mọi đối tượng.
4. **Bảo mật**: Token FCM phải được gắn chặt với Security Context của phiên đăng nhập đó. Khi Logout, hệ thống nên vô hiệu hóa token tương ứng.
