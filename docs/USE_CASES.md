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
