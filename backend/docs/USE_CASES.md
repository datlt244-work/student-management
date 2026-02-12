# Student Management System - Use Cases

## Tổng quan hệ thống

| Module | Mô tả |
|--------|-------|
| **Auth** | Xác thực, phân quyền người dùng |
| **User** | Quản lý tài khoản người dùng |
| **Department** | Quản lý khoa/bộ môn |
| **Teacher** | Quản lý giảng viên |
| **Student** | Quản lý sinh viên |
| **Course** | Quản lý môn học |
| **Class** | Quản lý lớp học phần |
| **Enrollment** | Đăng ký học phần |
| **Grade** | Quản lý điểm số |

---

## Module: Authentication

---

### UC-01: Đăng Nhập (Login) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/login` |
| **Actor** | Guest |
| **Mục đích** | Xác thực người dùng và cấp token |

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────┐
│  Client │      │ Auth Service │      │ PostgreSQL │      │ Redis │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └───┬───┘
     │                  │                     │                │
     │  1. POST /login  │                     │                │
     │─────────────────>│                     │                │
     │                  │                     │                │
     │                  │ 2. Check rate limit │                │
     │                  │────────────────────────────────────>│
     │                  │<────────────────────────────────────│
     │                  │                     │                │
     │                  │ 3. Find user by email               │
     │                  │────────────────────>│                │
     │                  │<────────────────────│                │
     │                  │                     │                │
     │                  │ 4. Check status (ACTIVE, verified)  │
     │                  │ 5. Verify password                  │
     │                  │ 6. Update login tracking            │
     │                  │────────────────────>│                │
     │                  │                     │                │
     │                  │ 7. Generate JWT + Refresh Token     │
     │                  │ 8. Store refresh token              │
     │                  │────────────────────────────────────>│
     │                  │                     │                │
     │ 9. Return tokens │                     │                │
     │<─────────────────│                     │                │
```

**Response (Success):**
```json
{
  "code": 1000,
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "uuid",
    "email": "user@example.com",
    "profilePictureUrl": null,
    "role": "STUDENT",
    "authenticated": true
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1300 | Invalid email or password | 401 |
| 1303 | Account is not active | 403 |
| 1304 | Account has been blocked | 403 |
| 1305 | Please verify your email | 403 |
| 1306 | Too many login attempts | 429 |

---

### UC-02: Đăng Ký Tài Khoản (Register)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/register` |
| **Actor** | Guest |
| **Mục đích** | Tạo tài khoản mới trong hệ thống |
| **Postcondition** | User được tạo với `status = PENDING_VERIFICATION`, `emailVerified = false` |
| **Next Step** | → UC-03 (Verify Email) → UC-01 (Login) |

**Trạng thái User sau đăng ký:**
```
┌─────────────────────────────────────────────────────────────────────────┐
│                        USER REGISTRATION FLOW                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   [UC-02: Register]              [UC-03: Verify]         [UC-01: Login] │
│         │                              │                       │        │
│         ▼                              ▼                       ▼        │
│   ┌─────────────┐    Email      ┌─────────────┐         ┌───────────┐  │
│   │   PENDING   │ ──────────► │   ACTIVE    │ ──────► │  Success  │  │
│   │ VERIFICATION│   Verify     │  emailVerified=true   │   Login   │  │
│   └─────────────┘              └─────────────┘         └───────────┘  │
│         │                                                               │
│         │ (Nếu login khi PENDING)                                       │
│         ▼                                                               │
│   ┌─────────────┐                                                       │
│   │ Error 1305  │  "Please verify your email before logging in"         │
│   └─────────────┘                                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ PostgreSQL │      │ Redis │      │ Email SMTP │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └───┬───┘      └─────┬──────┘
     │                  │                     │                │                │
     │  1. POST /register                     │                │                │
     │─────────────────>│                     │                │                │
     │                  │                     │                │                │
     │                  │ 2. Validate request (email format, password strength) │
     │                  │                     │                │                │
     │                  │ 3. Check email exists               │                │
     │                  │────────────────────>│                │                │
     │                  │<────────────────────│                │                │
     │                  │                     │                │                │
     │                  │ 4. Hash password (BCrypt)           │                │
     │                  │ 5. Create User:     │                │                │
     │                  │    - status = PENDING_VERIFICATION  │                │
     │                  │    - emailVerified = false          │                │
     │                  │────────────────────>│                │                │
     │                  │                     │                │                │
     │                  │ 6. Generate verification token (UUID)                │
     │                  │ 7. Store token in Redis (TTL: 15 phút)               │
     │                  │────────────────────────────────────>│                │
     │                  │                     │                │                │
     │                  │ 8. Send verification email          │                │
     │                  │    Link: /auth/verify-email?token=xxx               │
     │                  │─────────────────────────────────────────────────────>│
     │                  │                     │                │                │
     │ 9. Registration success               │                │                │
     │    (User CANNOT login yet!)           │                │                │
     │<─────────────────│                     │                │                │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid",
    "email": "user@example.com",
    "status": "PENDING_VERIFICATION",
    "emailVerified": false,
    "message": "Registration successful. Please check your email to verify your account."
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1200 | User already exists | 409 |
| 1100 | Email is required | 400 |
| 1101 | Invalid email format | 400 |
| 1120 | Password is required | 400 |
| 1122 | Password too weak | 400 |

**⚠️ Lưu ý quan trọng:**
- User **KHÔNG THỂ** login sau khi đăng ký (sẽ nhận error `1305: Please verify your email`)
- User **PHẢI** click link trong email để verify (UC-03)
- Sau khi verify thành công, user mới có thể login (UC-01)

---

### UC-03: Xác Thực Email (Verify Email)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /auth/verify-email?token={token}` |
| **Actor** | Guest (có token từ email) |
| **Mục đích** | Xác thực email người dùng |
| **Precondition** | User đã đăng ký (UC-02) và có `status = PENDING_VERIFICATION` |
| **Postcondition** | User có `status = ACTIVE`, `emailVerified = true` |
| **Next Step** | → UC-01 (Login) - User có thể đăng nhập |

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ Redis │      │ PostgreSQL │
└────┬────┘      └──────┬───────┘      └───┬───┘      └──────┬─────┘
     │                  │                  │                 │
     │ 1. GET /verify-email?token=xxx     │                 │
     │     (User click link từ email)     │                 │
     │─────────────────>│                  │                 │
     │                  │                  │                 │
     │                  │ 2. Validate token in Redis         │
     │                  │    (Check exists & not expired)    │
     │                  │─────────────────>│                 │
     │                  │<─────────────────│                 │
     │                  │                  │                 │
     │                  │ 3. Get userId from token           │
     │                  │ 4. Update user:   │                 │
     │                  │    - status = ACTIVE               │
     │                  │    - emailVerified = true          │
     │                  │    - emailVerifiedAt = now()       │
     │                  │────────────────────────────────────>
     │                  │                  │                 │
     │                  │ 5. Delete token from Redis         │
     │                  │    (Prevent reuse)                 │
     │                  │─────────────────>│                 │
     │                  │                  │                 │
     │ 6. Verification success            │                 │
     │    (User can now LOGIN!)           │                 │
     │<─────────────────│                  │                 │
```

**Response (Success):**
```json
{
  "code": 1000,
  "result": {
    "message": "Email verified successfully. You can now login."
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1181 | Token is invalid | 400 |
| 1182 | Token has expired | 401 |
| 1307 | Email is already verified | 400 |

**⚠️ Lưu ý:**
- Token chỉ sử dụng được **1 lần** (xóa sau khi verify)
- Token có **TTL 15 phút**, nếu hết hạn, user cần request resend email (UC-03.1)

---

### UC-03.1: Gửi Lại Email Xác Thực (Resend Verification Email)

| Thuộc tính | Giá trị |
|------------|--------|
| **Endpoint** | `POST /auth/resend-verification` |
| **Actor** | Guest |
| **Mục đích** | Gửi lại email xác thực khi chưa nhận được hoặc token hết hạn |
| **Precondition** | User có `status = PENDING_VERIFICATION` |
| **Rate Limit** | Tối đa **3 lần / 15 phút** |
| **Postcondition** | Token mới được tạo (TTL: 15p), token cũ bị xóa, email mới được gửi |

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ PostgreSQL │      │ Redis │      │ Email SMTP │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └───┬───┘      └─────┬──────┘
     │                  │                     │                │                │
     │ 1. POST /resend-verification           │                │                │
     │─────────────────>│                     │                │                │
     │                  │                     │                │                │
     │                  │ 2. Check rate limit (3 lần/15p)      │                │
     │                  │────────────────────────────────────>│                │
     │                  │<────────────────────────────────────│                │
     │                  │    → Nếu vượt quá → Error 1308      │                │
     │                  │                     │                │                │
     │                  │ 3. Find user by email               │                │
     │                  │────────────────────>│                │                │
     │                  │<────────────────────│                │                │
     │                  │                     │                │                │
     │                  │ 4. Check status == PENDING_VERIFICATION               │
     │                  │    → Nếu đã ACTIVE → Error 1307     │                │
     │                  │                     │                │                │
     │                  │ 5. Xóa token cũ (nếu có)            │                │
     │                  │ 6. Generate new verification token  │                │
     │                  │ 7. Store new token (TTL: 15 phút)   │                │
     │                  │ 8. Increment resend counter         │                │
     │                  │────────────────────────────────────>│                │
     │                  │                     │                │                │
     │                  │ 9. Send new verification email      │                │
     │                  │─────────────────────────────────────────────────────>│
     │                  │                     │                │                │
     │ 10. Success      │                     │                │                │
     │<─────────────────│                     │                │                │
```

**Response (Success):**
```json
{
  "code": 1000,
  "result": {
    "message": "Verification email sent. Please check your inbox.",
    "remainingAttempts": 2,
    "cooldownMinutes": 15
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |
| 1307 | Email is already verified | 400 |
| 1308 | Too many resend requests. Please try again in X minutes. | 429 |

**⚠️ Rate Limit Logic:**
```
Redis Keys:
  • resend:count:{email}  → Counter (số lần resend trong 15p)
  • verify:token:{userId} → Verification token (TTL: 15p)

Flow:
┌─────────────────────────────────────────────────────────────────┐
│ 1. GET resend:count:{email}                                     │
│    → Nếu >= 3: Trả về Error 1308 (Too many requests)            │
│    → Nếu < 3: Tiếp tục                                          │
│                                                                 │
│ 2. Check user status = PENDING_VERIFICATION                     │
│    → Nếu ACTIVE: Error 1307 (Already verified)                  │
│                                                                 │
│ 3. DELETE verify:token:{userId} (xóa token cũ nếu có)           │
│                                                                 │
│ 4. SET verify:token:{userId} = newToken (TTL: 15p)              │
│                                                                 │
│ 5. INCR resend:count:{email}                                    │
│    EXPIRE resend:count:{email} 900 (15 phút)                    │
│                                                                 │
│ 6. Send email với link verify                                   │
│                                                                 │
│ 7. Return success + remainingAttempts = 3 - count               │
└─────────────────────────────────────────────────────────────────┘
```

**Ví dụ Timeline:**
```
T+0:00  → Resend lần 1 ✅ (remaining: 2)
T+3:00  → Resend lần 2 ✅ (remaining: 1)  
T+5:00  → Resend lần 3 ✅ (remaining: 0)
T+7:00  → Resend lần 4 ❌ Error 1308 (chờ đến T+15:00)
T+15:00 → Counter reset → Resend lần 1 ✅ (remaining: 2)
```

---

### UC-04: Refresh Token

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/refresh-token` |
| **Actor** | Authenticated User |
| **Mục đích** | Làm mới access token |

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ Redis │      │ PostgreSQL │
└────┬────┘      └──────┬───────┘      └───┬───┘      └──────┬─────┘
     │                  │                  │                 │
     │ 1. POST /refresh-token              │                 │
     │─────────────────>│                  │                 │
     │                  │                  │                 │
     │                  │ 2. Validate refresh token          │
     │                  │─────────────────>│                 │
     │                  │<─────────────────│                 │
     │                  │                  │                 │
     │                  │ 3. Get user info │                 │
     │                  │─────────────────────────────────────>
     │                  │<─────────────────────────────────────
     │                  │                  │                 │
     │                  │ 4. Delete old refresh token        │
     │                  │ 5. Generate new tokens             │
     │                  │ 6. Store new refresh token         │
     │                  │─────────────────>│                 │
     │                  │                  │                 │
     │ 7. Return new tokens                │                 │
     │<─────────────────│                  │                 │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "new-refresh-token-uuid",
    "expiresIn": 3600
  }
}
```

---

### UC-05: Đăng Xuất (Logout)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/logout` |
| **Actor** | Authenticated User |
| **Mục đích** | Đăng xuất và hủy token |

**Request Header:**
```
Authorization: Bearer {accessToken}
```

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌───────┐
│  Client │      │ Auth Service │      │ Redis │
└────┬────┘      └──────┬───────┘      └───┬───┘
     │                  │                  │
     │ 1. POST /logout  │                  │
     │─────────────────>│                  │
     │                  │                  │
     │                  │ 2. Validate JWT from header
     │                  │ 3. Delete refresh token
     │                  │─────────────────>│
     │                  │                  │
     │                  │ 4. Add JWT to blacklist (optional)
     │                  │─────────────────>│
     │                  │                  │
     │ 5. Logout success│                  │
     │<─────────────────│                  │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "message": "Logged out successfully"
  }
}
```

---

### UC-06: Quên Mật Khẩu (Forgot Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/forgot-password` |
| **Actor** | Guest |
| **Mục đích** | Gửi email reset mật khẩu |
| **Rate Limit** | Tối đa **3 lần / 15 phút** (theo email) |
| **Security** | Không tiết lộ email có tồn tại hay không |

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ PostgreSQL │      │ Redis │      │ Email SMTP │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └───┬───┘      └─────┬──────┘
     │                  │                     │                │                │
     │ 1. POST /forgot-password               │                │                │
     │─────────────────>│                     │                │                │
     │                  │                     │                │                │
     │                  │ 2. Check rate limit (3 lần/15p)      │                │
     │                  │────────────────────────────────────>│                │
     │                  │<────────────────────────────────────│                │
     │                  │    → Nếu vượt quá → Error 1309      │                │
     │                  │                     │                │                │
     │                  │ 3. Find user by email               │                │
     │                  │────────────────────>│                │                │
     │                  │<────────────────────│                │                │
     │                  │                     │                │                │
     │                  │ 4. Nếu user KHÔNG tồn tại:          │                │
     │                  │    → Increment rate limit counter   │                │
     │                  │    → Return SUCCESS (không gửi email)│               │
     │                  │    (Không tiết lộ email không tồn tại)               │
     │                  │                     │                │                │
     │                  │ 5. Nếu user TỒN TẠI:                │                │
     │                  │    → Delete old reset token (nếu có)│                │
     │                  │    → Generate new reset token       │                │
     │                  │    → Store token (TTL: 15 phút)     │                │
     │                  │    → Increment rate limit counter   │                │
     │                  │────────────────────────────────────>│                │
     │                  │                     │                │                │
     │                  │    → Send reset email               │                │
     │                  │─────────────────────────────────────────────────────>│
     │                  │                     │                │                │
     │ 6. Success (always same message)       │                │                │
     │<─────────────────│                     │                │                │
```

**Response (Success - Luôn giống nhau):**
```json
{
  "code": 1000,
  "result": {
    "message": "If an account exists with this email, a password reset link has been sent.",
    "cooldownMinutes": 15
  }
}
```

**Error Response (Chỉ khi rate limit):**
```json
{
  "code": 1309,
  "message": "Too many password reset requests. Please try again in X minutes."
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1309 | Too many password reset requests. Please try again in X minutes. | 429 |

**⚠️ Logic Hybrid (Security + UX):**
```
┌─────────────────────────────────────────────────────────────────────────┐
│                     FORGOT PASSWORD - HYBRID LOGIC                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Redis Keys:                                                            │
│  • reset:count:{email}   → Counter rate limit (TTL: 15p)                │
│  • reset:token:{userId}  → Reset token (TTL: 15p)                       │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Step 1: Check rate limit TRƯỚC                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ GET reset:count:{email}                                         │    │
│  │ → Nếu >= 3: Error 1309 "Too many requests" (BÁO CHO USER)       │    │
│  │ → Nếu < 3: Tiếp tục                                             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  Step 2: Find user by email                                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ SELECT * FROM users WHERE email = ?                             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  Step 3: Xử lý theo kết quả                                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ CASE A: User KHÔNG tồn tại                                      │    │
│  │   → INCR reset:count:{email} (vẫn đếm để chống brute force)     │    │
│  │   → Return SUCCESS (KHÔNG tiết lộ email không tồn tại)          │    │
│  │   → KHÔNG gửi email                                             │    │
│  │                                                                 │    │
│  │ CASE B: User TỒN TẠI                                            │    │
│  │   → DELETE reset:token:{userId} (xóa token cũ)                  │    │
│  │   → SET reset:token:{userId} = newToken (TTL: 15p)              │    │
│  │   → INCR reset:count:{email}                                    │    │
│  │   → Send email với link reset                                   │    │
│  │   → Return SUCCESS                                              │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  ⚠️ Response message LUÔN GIỐNG NHAU cho cả 2 case!                    │
│  "If an account exists with this email, a reset link has been sent."   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Ví dụ Timeline:**
```
Email tồn tại:
  T+0:00  → Request 1 ✅ Email sent, Success message
  T+2:00  → Request 2 ✅ Email sent (token cũ bị xóa), Success message
  T+4:00  → Request 3 ✅ Email sent, Success message
  T+5:00  → Request 4 ❌ Error 1309 "Too many requests"
  T+15:00 → Counter reset → Request 1 ✅

Email KHÔNG tồn tại (hacker thử):
  T+0:00  → Request 1 ✅ Success message (nhưng không gửi email)
  T+1:00  → Request 2 ✅ Success message (nhưng không gửi email)
  T+2:00  → Request 3 ✅ Success message (nhưng không gửi email)
  T+3:00  → Request 4 ❌ Error 1309 "Too many requests"
  
→ Hacker không biết email có tồn tại hay không!
```

---

### UC-07: Đặt Lại Mật Khẩu (Reset Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /auth/reset-password` |
| **Actor** | Guest (có token từ email) |
| **Mục đích** | Đặt lại mật khẩu mới |
| **Security** | Invalidate tất cả sessions sau khi đổi password |

**Request Body:**
```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌───────┐      ┌────────────┐
│  Client │      │ Auth Service │      │ Redis │      │ PostgreSQL │
└────┬────┘      └──────┬───────┘      └───┬───┘      └──────┬─────┘
     │                  │                  │                 │
     │ 1. POST /reset-password             │                 │
     │─────────────────>│                  │                 │
     │                  │                  │                 │
     │                  │ 2. Validate passwords match        │
     │                  │ 3. Validate reset token            │
     │                  │─────────────────>│                 │
     │                  │<─────────────────│                 │
     │                  │                  │                 │
     │                  │ 4. Get userId from token           │
     │                  │ 5. Hash new password               │
     │                  │ 6. Update password_hash            │
     │                  │────────────────────────────────────>
     │                  │                  │                 │
     │                  │ 7. Delete reset token              │
     │                  │ 8. Delete ALL refresh tokens       │
     │                  │    (Logout all devices)            │
     │                  │─────────────────>│                 │
     │                  │                  │                 │
     │ 9. Reset success │                  │                 │
     │<─────────────────│                  │                 │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "message": "Password reset successfully. All sessions have been logged out. Please login again."
  }
}
```

**⚠️ Security: Blacklist JWT + Logout All Devices:**
```
┌─────────────────────────────────────────────────────────────────────────┐
│              BLACKLIST APPROACH (Đơn giản cho dự án nhỏ)                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Redis Keys:                                                            │
│  • blacklist:jwt:{jti}      → Blacklisted JWT IDs (TTL = remaining exp) │
│  • refresh:token:{userId}   → Refresh token của user                    │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Bước 1: Update password trong DB                                       │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ UPDATE users SET password_hash = ? WHERE user_id = ?            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  Bước 2: Xóa tất cả refresh tokens của user                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ // Nếu lưu refresh token theo userId                            │    │
│  │ DEL refresh:token:{userId}                                      │    │
│  │                                                                 │    │
│  │ // Hoặc nếu lưu nhiều device                                    │    │
│  │ KEYS refresh:token:{userId}:* → DEL all                         │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  Bước 3: (Optional) Blacklist JWT hiện tại nếu biết                     │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ // Không cần nếu chỉ xóa refresh token                          │    │
│  │ // JWT sẽ tự hết hạn sau 1h (hoặc expiration time)              │    │
│  │                                                                 │    │
│  │ // Nhưng nếu muốn invalidate ngay:                              │    │
│  │ SETEX blacklist:jwt:{jti} {remainingTTL} "1"                    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  JWT Validation (trong JwtAuthFilter):                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ String jti = jwt.getJWTClaimsSet().getJWTID();                  │    │
│  │ Boolean isBlacklisted = redis.exists("blacklist:jwt:" + jti);   │    │
│  │                                                                 │    │
│  │ if (isBlacklisted) {                                            │    │
│  │     throw new AppException(ErrorCode.TOKEN_BLACKLISTED);        │    │
│  │ }                                                               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1181 | Token is invalid | 400 |
| 1182 | Token has expired | 401 |
| 1310 | Passwords do not match | 400 |
| 1122 | Password too weak | 400 |

---

---

## Module: User Management

---

### UC-08: Lấy Thông Tin Profile (Combined Profile) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /profile/me` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem thông tin đầy đủ: User + Student/Teacher profile |
| **Đặc điểm** | Trả về tất cả thông tin trong 1 API call dựa theo role |

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│  Client │      │ Profile Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. GET /profile/me │                      │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Extract userId + role from JWT
     │                    │                      │
     │                    │ 3. Fetch User data   │
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │                    │ 4. Switch by role:   │
     │                    │    IF role == STUDENT:│
     │                    │       Fetch Student + Department
     │                    │       Fetch Current Semester
     │                    │    ELSE IF role == TEACHER:
     │                    │       Fetch Teacher + Department
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 5. Return combined profile               │
     │<───────────────────│                      │
```

**Response cho STUDENT:**
```json
{
  "code": 1000,
  "result": {
    // ========== TỪ BẢNG USERS ==========
    "userId": "uuid-123",
    "email": "student@school.edu",
    "profilePictureUrl": "https://example.com/avatar.jpg",
    "role": "student",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2026-02-02T21:00:00",
    "loginCount": 15,
    "createdAt": "2026-01-01T10:00:00",
    
    // ========== TỪ BẢNG STUDENTS ==========
    "studentProfile": {
      "studentId": "uuid",
      "studentCode": "HE170001",
      "firstName": "Nguyễn",
      "lastName": "Văn A",
      "dob": "2000-05-15",
      "gender": "MALE",
      "major": "Software Engineering",
      "phone": "0909123456",
      "address": "123 ABC Street, HCM City",
      "gpa": 3.45,
      "department": {
        "departmentId": 1,
        "name": "Công nghệ Thông tin",
        "officeLocation": "Building A, Room 101"
      },
      "currentSemester": {
        "semesterId": 1,
        "name": "SPRING",
        "year": 2026,
        "displayName": "Spring 2026",
        "startDate": "2026-01-12",
        "endDate": "2026-05-10"
      }
    },
    
    // null nếu không phải Teacher
    "teacherProfile": null
  }
}
```

**Response cho TEACHER:**
```json
{
  "code": 1000,
  "result": {
    // ========== TỪ BẢNG USERS ==========
    "userId": "uuid-456",
    "email": "teacher@school.edu",
    "profilePictureUrl": null,
    "role": "teacher",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2026-02-03T08:00:00",
    "loginCount": 42,
    "createdAt": "2025-06-01T10:00:00",
    
    // null nếu không phải Student
    "studentProfile": null,
    
    // ========== TỪ BẢNG TEACHERS ==========
    "teacherProfile": {
      "teacherId": "uuid",
      "teacherCode": "HJ170001",
      "firstName": "Trần",
      "lastName": "Văn B",
      "phone": "0901234567",
      "specialization": "Machine Learning",
      "academicRank": "Associate Professor",
      "officeRoom": "Building C, Room 402",
      "department": {
        "departmentId": 1,
        "name": "Công nghệ Thông tin",
        "officeLocation": "Building A, Room 101"
      }
    }
  }
}
```

**Response cho ADMIN:**
```json
{
  "code": 1000,
  "result": {
    // ========== TỪ BẢNG USERS ==========
    "userId": "uuid-789",
    "email": "admin@school.edu",
    "profilePictureUrl": null,
    "role": "admin",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2026-02-03T00:00:00",
    "loginCount": 100,
    "createdAt": "2025-01-01T00:00:00",
    
    // Admin không có Student/Teacher profile
    "studentProfile": null,
    "teacherProfile": null
  }
}
```

**Database Tables:**

**Bảng `students` (các field liên quan profile):**

| Column | Type | Mô tả |
|--------|------|-------|
| student_id | UUID (PK) | ID sinh viên |
| user_id | UUID (FK → users) | Liên kết User |
| department_id | INT (FK → departments) | Khoa |
| student_code | VARCHAR(10) UNIQUE | Mã SV dạng `HExxxxxx` |
| first_name | VARCHAR(50) | Họ |
| last_name | VARCHAR(50) | Tên |
| dob | DATE | Ngày sinh |
| gender | VARCHAR(10) | `MALE`, `FEMALE`, `OTHER` |
| major | VARCHAR(100) | Chuyên ngành |
| email | VARCHAR(100) | Email sinh viên |
| phone | VARCHAR(20) | Số điện thoại |
| address | VARCHAR(255) | Địa chỉ |
| gpa | DECIMAL(3,2) | GPA thang 4.0 |

**Bảng `teachers` (các field liên quan profile):**

| Column | Type | Mô tả |
|--------|------|-------|
| teacher_id | UUID (PK) | ID giảng viên |
| user_id | UUID (FK → users) | Liên kết User |
| department_id | INT (FK → departments) | Khoa |
| teacher_code | VARCHAR(10) UNIQUE | Mã GV dạng `HJxxxxxx` |
| first_name | VARCHAR(50) | Họ |
| last_name | VARCHAR(50) | Tên |
| email | VARCHAR(100) | Email giảng viên |
| phone | VARCHAR(20) | Số điện thoại |
| specialization | VARCHAR(100) | Chuyên môn |
| academic_rank | VARCHAR(50) | Học hàm/học vị |
| office_room | VARCHAR(50) | Phòng làm việc |

**Bảng `semesters`:**

| Column | Type | Mô tả |
|--------|------|-------|
| semester_id | INT (PK) | Auto-increment |
| name | VARCHAR(20) | `SPRING`, `SUMMER`, `FALL` |
| year | INT | Năm học |
| start_date | DATE | Ngày bắt đầu kỳ |
| end_date | DATE | Ngày kết thúc kỳ |
| is_current | BOOLEAN | Kỳ hiện tại (chỉ 1 record = true) |

> Mỗi năm có 3 kỳ: Spring, Summer, Fall. Unique constraint trên `(name, year)`.

**SQL Query (cho Student):**
```sql
-- Query 1: Get User
SELECT u.*, r.role_name 
FROM users u 
JOIN roles r ON u.role_id = r.role_id 
WHERE u.user_id = :userId;

-- Query 2: Get Student profile (nếu role = STUDENT)
SELECT s.*, d.name as department_name, d.office_location
FROM students s
LEFT JOIN departments d ON s.department_id = d.department_id
WHERE s.user_id = :userId;

-- Query 3: Get current semester
SELECT * FROM semesters WHERE is_current = TRUE;
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |
| 1501 | Student profile not found (nếu role=STUDENT nhưng chưa có profile) | 404 |
| 1502 | Teacher profile not found (nếu role=TEACHER nhưng chưa có profile) | 404 |

**⚠️ Lưu ý quan trọng:**
- Đây là **endpoint duy nhất** để lấy thông tin profile
- Frontend chỉ cần gọi **1 API** thay vì 2 API riêng biệt
- Response structure **luôn giống nhau**, chỉ khác ở `studentProfile` hoặc `teacherProfile` có data hay null
- Student profile bao gồm `currentSemester` (kỳ hiện tại từ bảng semesters)
- Mã sinh viên dạng `HExxxxxx`, mã giảng viên dạng `HJxxxxxx`

**Editable Fields trên Frontend:**

| Role | Field | Editable by User |
|------|-------|------------------|
| STUDENT | phone | ✅ |
| STUDENT | address | ✅ |
| STUDENT | fullName, dob, gender, major, studentCode, email | ❌ (Admin only) |
| TEACHER | phone | ✅ |
| TEACHER | academicRank, specialization, officeRoom, teacherCode, email | ❌ (Admin only) |

---

### UC-09: Cập Nhật Profile (Combined Profile Update) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /profile/me` |
| **Actor** | Authenticated User |
| **Mục đích** | Cập nhật thông tin Student/Teacher trong 1 API call |
| **Đặc điểm** | Partial update - chỉ update các field được gửi |

**Request Body cho STUDENT (phone + address):**
```json
{
  "studentProfile": {
    "phone": "0909123456",
    "address": "456 XYZ Street, HCM City"
  }
}
```

**Request Body cho TEACHER (phone only):**
```json
{
  "teacherProfile": {
    "phone": "0901234567"
  }
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│  Client │      │ Profile Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. PUT /profile/me │                      │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Extract userId + role from JWT
     │                    │ 3. Validate request  │
     │                    │    - Student gửi teacherProfile → 1402
     │                    │    - Teacher gửi studentProfile → 1401
     │                    │                      │
     │                    │ 4. IF role == STUDENT && có studentProfile:
     │                    │       Update students.phone, students.address
     │                    │    ELSE IF role == TEACHER && có teacherProfile:
     │                    │       Update teachers.phone
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │                    │ 5. Reload combined profile (getMyProfile)
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 6. Return updated combined profile       │
     │    (same format as GET /profile/me)      │
     │<───────────────────│                      │
```

**Response (cho Student - same format as GET /profile/me):**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-123",
    "email": "student@school.edu",
    "profilePictureUrl": null,
    "role": "student",
    "status": "ACTIVE",
    "emailVerified": true,
    "lastLoginAt": "2026-02-02T21:00:00",
    "loginCount": 15,
    "createdAt": "2026-01-01T10:00:00",
    
    "studentProfile": {
      "studentId": "uuid",
      "studentCode": "HE170001",
      "firstName": "Nguyễn",
      "lastName": "Văn A",
      "dob": "2000-05-15",
      "gender": "MALE",
      "major": "Software Engineering",
      "phone": "0909123456",
      "address": "456 XYZ Street, HCM City",
      "gpa": 3.45,
      "department": {
        "departmentId": 1,
        "name": "Công nghệ Thông tin",
        "officeLocation": "Building A, Room 101"
      },
      "currentSemester": {
        "semesterId": 1,
        "name": "SPRING",
        "year": 2026,
        "displayName": "Spring 2026",
        "startDate": "2026-01-12",
        "endDate": "2026-05-10"
      }
    },
    
    "teacherProfile": null
  }
}
```

**Quy tắc Editable Fields:**

| Role | Field | Editable by User |
|------|-------|------------------|
| STUDENT | studentProfile.phone | ✅ |
| STUDENT | studentProfile.address | ✅ |
| STUDENT | firstName, lastName, dob, gender, major, studentCode, email, departmentId | ❌ (Admin only) |
| TEACHER | teacherProfile.phone | ✅ |
| TEACHER | firstName, lastName, academicRank, specialization, officeRoom, teacherCode, email, departmentId | ❌ (Admin only) |

**Validation Rules:**

| Field | Rule |
|-------|------|
| phone | Max 20 characters |
| address | Max 255 characters |

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1401 | Access denied - Student role required (Teacher gửi studentProfile) | 403 |
| 1402 | Access denied - Teacher role required (Student gửi teacherProfile) | 403 |
| 1501 | Student profile not found | 404 |
| 1502 | Teacher profile not found | 404 |

**⚠️ Lưu ý quan trọng:**
- **Partial update**: Chỉ update các field được gửi, field null sẽ bị bỏ qua
- Response format **giống hệt** `GET /profile/me`
- **Cross-role validation**: Student không thể gửi `teacherProfile` và ngược lại
- Frontend hiển thị các field không editable với style `disabled` + `cursor-not-allowed`

---

### UC-09.1: Cập Nhật Student Profile

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /students/me` |
| **Actor** | Student |
| **Mục đích** | Cập nhật thông tin hồ sơ sinh viên |
| **Precondition** | User có role STUDENT và đã có record trong bảng students |

**Request Body:**
```json
{
  "phone": "0909123456",
  "address": "456 XYZ Street, HCM City"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│ Student │      │ Student Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. PUT /students/me                       │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Check role == STUDENT
     │                    │ 3. Validate request  │
     │                    │ 4. Update student    │
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 5. Return updated profile                 │
     │<───────────────────│                      │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "studentId": 1,
    "firstName": "Tran",
    "lastName": "Van C",
    "dob": "2000-05-15",
    "email": "student.c@school.edu",
    "phone": "0909123456",
    "address": "456 XYZ Street, HCM City",
    "department": {
      "departmentId": 1,
      "name": "Công nghệ Thông tin"
    },
    "updatedAt": "2026-02-03T00:19:00"
  }
}
```

**Allowed Fields (Student tự sửa):**

| Field | Editable by Student |
|-------|---------------------|
| phone | ✅ |
| address | ✅ |
| firstName | ❌ (Admin only) |
| lastName | ❌ (Admin only) |
| dob | ❌ (Admin only) |
| email | ❌ (Admin only) |
| departmentId | ❌ (Admin only) |

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1401 | Access denied - Student role required | 403 |
| 1501 | Student profile not found | 404 |

---

### UC-09.2: Cập Nhật Teacher Profile

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /teachers/me` |
| **Actor** | Teacher |
| **Mục đích** | Cập nhật thông tin hồ sơ giảng viên |
| **Precondition** | User có role TEACHER và đã có record trong bảng teachers |

**Request Body:**
```json
{
  "phone": "0901234567",
  "specialization": "Artificial Intelligence"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│ Teacher │      │ Teacher Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. PUT /teachers/me                       │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Check role == TEACHER
     │                    │ 3. Validate request  │
     │                    │ 4. Update teacher    │
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 5. Return updated profile                 │
     │<───────────────────│                      │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "teacherId": 1,
    "firstName": "Nguyen",
    "lastName": "Van A",
    "email": "teacher.a@school.edu",
    "phone": "0901234567",
    "specialization": "Artificial Intelligence",
    "department": {
      "departmentId": 1,
      "name": "Công nghệ Thông tin"
    },
    "updatedAt": "2026-02-03T00:19:00"
  }
}
```

**Allowed Fields (Teacher tự sửa):**

| Field | Editable by Teacher |
|-------|---------------------|
| phone | ✅ |
| specialization | ✅ |
| firstName | ❌ (Admin only) |
| lastName | ❌ (Admin only) |
| email | ❌ (Admin only) |
| departmentId | ❌ (Admin only) |

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1401 | Access denied - Teacher role required | 403 |
| 1502 | Teacher profile not found | 404 |

---

### UC-09.3: Upload Ảnh Đại Diện (Upload Profile Image) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /profile/me/avatar` |
| **Actor** | Authenticated User (Student, Teacher, Admin) |
| **Mục đích** | Upload hoặc thay thế ảnh đại diện (profile picture) |
| **Content-Type** | `multipart/form-data` |
| **Storage** | MinIO - Bucket: `student-management`, Prefix: `avatar/` |
| **Postcondition** | `users.profile_picture_url` được cập nhật với URL ảnh mới |

#### 1. Chiến Lược Đặt Tên (Naming Strategy)

Sử dụng **UUID** trong tên file để đảm bảo mỗi lần upload tạo ra URL mới hoàn toàn:

```
Cấu trúc đường dẫn: avatar/{user_id}/{uuid}.webp

Ví dụ:
  Upload lần 1: avatar/550e8400-e29b-41d4.../a1b2c3d4-e5f6-7890.webp
  Upload lần 2: avatar/550e8400-e29b-41d4.../f9e8d7c6-b5a4-3210.webp
  → URL khác nhau → Trình duyệt tự tải ảnh mới, không cần xóa cache
```

> **Tại sao không dùng tên cố định (ví dụ `user_123.jpg`)?**
> - CDN/Browser cache ảnh cũ → user đổi ảnh nhưng vẫn thấy ảnh cũ
> - Phải thêm query string `?v=timestamp` → không chuyên nghiệp
> - UUID tên file = mỗi upload là URL mới = cache-busting tự nhiên

#### 2. Validation

| Rule | Giá trị |
|------|---------|
| **Max file size** | 5 MB |
| **Allowed formats** | JPG, JPEG, PNG, WebP |
| **Content-Type check** | Kiểm tra cả extension VÀ magic bytes (file signature) |

```
Magic Bytes:
  JPEG: FF D8 FF
  PNG:  89 50 4E 47 0D 0A 1A 0A
  WebP: 52 49 46 46 xx xx xx xx 57 45 42 50
```

#### 3. Image Processing (Backend)

Trước khi upload lên MinIO, Backend **phải** xử lý ảnh:

```
┌─────────────────────────────────────────────────────────────────┐
│                     IMAGE PROCESSING PIPELINE                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Input: Raw image từ user (có thể vài MB, kích thước bất kỳ)    │
│                                                                  │
│  Step 1: Resize                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ - Max dimensions: 500x500 px                              │  │
│  │ - Giữ tỉ lệ (aspect ratio), crop center nếu cần          │  │
│  │ - Nếu ảnh nhỏ hơn 500x500 → giữ nguyên                   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Step 2: Convert & Compress                                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ - Convert sang WebP (quality 80%)                         │  │
│  │ - Kết quả: file nhỏ hơn nhiều so với ảnh gốc              │  │
│  │ - Ví dụ: 3MB JPEG → ~50-100KB WebP                       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Output: {uuid}.webp (optimized, sẵn sàng upload MinIO)        │
│                                                                  │
│  Java Library: Thumbnailator hoặc ImageIO + WebP encoder        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 4. Luồng Hoạt Động (Atomic Replace Flow)

```
┌─────────┐      ┌─────────────────┐      ┌────────────┐      ┌───────┐
│  Client │      │ Profile Service │      │ PostgreSQL │      │ MinIO │
└────┬────┘      └────────┬────────┘      └──────┬─────┘      └───┬───┘
     │                    │                      │                │
     │ 1. POST /profile/me/avatar               │                │
     │    [multipart/form-data: file]            │                │
     │───────────────────>│                      │                │
     │                    │                      │                │
     │                    │ 2. VALIDATE:         │                │
     │                    │    - File size <= 5MB │                │
     │                    │    - Format: JPG/PNG/WebP             │
     │                    │    - Magic bytes check│                │
     │                    │    → Nếu fail → Error (return ngay)   │
     │                    │                      │                │
     │                    │ 3. IMAGE PROCESSING: │                │
     │                    │    - Resize (max 500x500)             │
     │                    │    - Convert to WebP (quality 80%)    │
     │                    │    - Generate filename: {uuid}.webp   │
     │                    │                      │                │
     │                    │ 4. GET old avatar URL from DB         │
     │                    │───────────────────────>               │
     │                    │<───────────────────────               │
     │                    │                      │                │
     │                    │ 5. UPLOAD NEW to MinIO                │
     │                    │    Path: avatar/{userId}/{uuid}.webp  │
     │                    │    Metadata:          │                │
     │                    │      Content-Type: image/webp         │
     │                    │      Cache-Control: public, max-age=31536000, immutable
     │                    │──────────────────────────────────────>│
     │                    │<──────────────────────────────────────│
     │                    │    → Nếu upload fail → Error (return) │
     │                    │                      │                │
     │                    │ 6. UPDATE DB:         │                │
     │                    │    users.profile_picture_url = newUrl │
     │                    │───────────────────────>               │
     │                    │<───────────────────────               │
     │                    │    → Nếu DB fail:     │                │
     │                    │      DELETE ảnh mới trên MinIO (cleanup)
     │                    │      → Error          │                │
     │                    │                      │                │
     │                    │ 7. DELETE OLD from MinIO (nếu có)     │
     │                    │    (Extract path từ old URL)          │
     │                    │──────────────────────────────────────>│
     │                    │    → Nếu delete fail: log warning     │
     │                    │      (chấp nhận rác, dữ liệu user OK)│
     │                    │                      │                │
     │ 8. Return new avatar URL                  │                │
     │<───────────────────│                      │                │
```

#### 5. Atomic Logic (Error Handling)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     ATOMIC REPLACE - ERROR HANDLING                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Nguyên tắc: Dữ liệu người dùng (DB) phải LUÔN ĐÚNG                    │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ CASE 1: Upload MinIO THẤT BẠI                                     │  │
│  │   → Return Error ngay                                             │  │
│  │   → Không thay đổi gì trong DB                                    │  │
│  │   → Trạng thái: Giữ nguyên ảnh cũ ✅                              │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ CASE 2: Upload OK, Update DB THẤT BẠI                             │  │
│  │   → XÓA ảnh mới vừa upload trên MinIO (cleanup orphan)           │  │
│  │   → Return Error                                                  │  │
│  │   → Trạng thái: Giữ nguyên ảnh cũ ✅, không có file rác ✅        │  │
│  ├───────────────────────────────────────────────────────────────────┤  │
│  │ CASE 3: Upload OK, Update DB OK, Delete ảnh cũ THẤT BẠI          │  │
│  │   → Log warning (không throw error)                               │  │
│  │   → Return Success                                                │  │
│  │   → Trạng thái: DB đúng ✅, ảnh cũ thành orphan (rác)             │  │
│  │   → Giải pháp: Background job dọn rác định kỳ (optional)         │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ⚠️ Ưu tiên: DB consistency > Storage cleanup                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 6. MinIO Configuration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         MINIO BUCKET SETUP                              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Bucket: student-management                                             │
│  Prefix public: avatar/*                                                │
│                                                                         │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ Access Policy cho prefix avatar/*: PUBLIC DOWNLOAD                │  │
│  │                                                                   │  │
│  │ Command:                                                          │  │
│  │   mc anonymous set download mylocal/student-management/avatar     │  │
│  │                                                                   │  │
│  │ → Client truy cập trực tiếp: http://{minio-host}:9000/           │  │
│  │   student-management/avatar/{userId}/{uuid}.webp                  │  │
│  │                                                                   │  │
│  │ → Không cần Presigned URL cho avatar                              │  │
│  │ → Giảm tải cho Backend (không cần proxy ảnh)                      │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  Upload Metadata:                                                       │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │ Content-Type:  image/webp                                         │  │
│  │ Cache-Control: public, max-age=31536000, immutable                │  │
│  │                                                                   │  │
│  │ → max-age=31536000 = cache 1 năm                                  │  │
│  │ → immutable = trình duyệt KHÔNG cần revalidate                   │  │
│  │ → An toàn vì mỗi upload = URL mới (UUID filename)                │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 7. URL Construction

```
URL Pattern:
  {minio-endpoint}/{bucket}/{prefix}/{userId}/{uuid}.webp

Ví dụ (Development):
  http://localhost:9000/student-management/avatar/550e8400.../a1b2c3d4.webp

Ví dụ (Production - qua reverse proxy / CDN):
  https://cdn.school.edu/avatar/550e8400.../a1b2c3d4.webp

Lưu trong DB:
  users.profile_picture_url = "avatar/550e8400.../a1b2c3d4.webp"
  → Chỉ lưu relative path, construct full URL ở Backend/Frontend
  → Dễ dàng migrate storage hoặc đổi domain sau này
```

**Request (multipart/form-data):**
```
POST /profile/me/avatar
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="my_photo.jpg"
Content-Type: image/jpeg

[binary data]
------WebKitFormBoundary--
```

**Response (Success):**
```json
{
  "code": 1000,
  "result": {
    "profilePictureUrl": "avatar/550e8400-e29b-41d4.../a1b2c3d4-e5f6-7890.webp",
    "fullUrl": "http://localhost:9000/student-management/avatar/550e8400.../a1b2c3d4.webp",
    "message": "Profile picture updated successfully"
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1600 | File is required | 400 |
| 1601 | File size exceeds maximum limit (5MB) | 400 |
| 1602 | Invalid file format. Allowed: JPG, PNG, WebP | 400 |
| 1603 | Failed to process image | 500 |
| 1604 | Failed to upload file to storage | 500 |

**⚠️ Lưu ý quan trọng:**
- **Naming**: UUID filename → cache-busting tự nhiên, không cần query string
- **Processing**: Luôn resize + convert WebP ở Backend trước khi lưu
- **Atomic**: Upload trước → Update DB → Delete ảnh cũ (theo thứ tự)
- **Rollback**: Nếu DB fail → xóa ảnh mới; Nếu delete ảnh cũ fail → chấp nhận rác
- **Storage**: Chỉ lưu relative path trong DB, construct full URL khi cần
- **Cache**: `immutable` cache header + UUID filename = tối ưu performance

**Implementation notes (đã làm):**
- DB lưu relative path (`avatar/{userId}/{uuid}.webp`). API **GET /profile/me** và **login/refresh** trả về `profilePictureUrl` dạng **full URL** (gọi `IStorageService.getPublicUrl(relativePath)`) để frontend hiển thị avatar đúng sau khi reload trang.
- Upload response vẫn trả `profilePictureUrl` (relative) + `fullUrl` (full); frontend dùng `fullUrl` ngay sau upload. Khi load lại trang, `GET /profile/me` trả full URL nên ảnh không biến mất.

---

### UC-10: Đổi Mật Khẩu (Change Password) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /users/me/change-password` |
| **Actor** | Authenticated User |
| **Mục đích** | Thay đổi mật khẩu |
| **Security** | Logout toàn bộ thiết bị/tab ngay (token version + xóa refresh token). Client redirect về login sau khi đổi thành công. |

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!",
  "logoutOtherDevices": true
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐      ┌───────┐
│  Client │      │ User Service │      │ PostgreSQL │      │ Redis │
└────┬────┘      └──────┬───────┘      └──────┬─────┘      └───┬───┘
     │                  │                     │                │
     │ 1. POST /users/me/change-password      │                │
     │─────────────────>│                     │                │
     │                  │                     │                │
     │                  │ 2. Validate newPassword == confirmPassword
     │                  │ 3. Fetch current user               │
     │                  │────────────────────>│                │
     │                  │<────────────────────│                │
     │                  │                     │                │
     │                  │ 4. Verify current password           │
     │                  │ 5. Check new != old │                │
     │                  │ 6. Hash new password│                │
     │                  │ 7. Update password_hash              │
     │                  │────────────────────>│                │
     │                  │                     │                │
     │                  │ 8. Increment token version (Redis)   │
     │                  │    → Mọi JWT cũ (mọi tab/thiết bị)  │
     │                  │      bị từ chối ngay khi validate   │
     │                  │────────────────────────────────────>│
     │                  │ 9. If logoutOtherDevices == true:    │
     │                  │    → Delete ALL refresh tokens       │
     │                  │────────────────────────────────────>│
     │                  │                     │                │
     │ 10. Success      │                     │                │
     │     (không cấp token mới; client logout & redirect login)
     │<─────────────────│                     │                │
```

**Response (đã implement):**
```json
{
  "code": 1000,
  "result": {
    "message": "Password changed successfully. Please login again.",
    "loggedOutDevices": 3
  }
}
```

**⚠️ Security Logic (đã implement – Token Version + Logout all):**
```
┌─────────────────────────────────────────────────────────────────────────┐
│            CHANGE PASSWORD - TOKEN VERSION + LOGOUT ALL                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. Update password trong DB                                            │
│  2. Increment token version (Redis: auth:token-version:{userId})        │
│     → JWT có claim "tv" (token version). Validator so sánh với Redis.  │
│     → Mọi request dùng JWT cũ (tv < current) → 401 ngay.                │
│     → Tab ẩn danh / thiết bị khác hết hiệu lực ngay, không chờ hết hạn. │
│  3. Delete ALL refresh tokens (logoutOtherDevices default true)          │
│  4. Không cấp newAccessToken/newRefreshToken                            │
│     → Frontend sau khi đổi thành công: clearAuth() + redirect /login    │
│                                                                         │
│  Kết quả: Thiết bị đổi mật khẩu → logout và chuyển login; mọi tab/      │
│  thiết bị khác → request tiếp theo 401 → FE refresh thất bại → redirect  │
│  login.                                                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

**Implementation notes (đã làm):**
- Backend: `UserController` POST `/users/me/change-password`, `AuthService.changePassword()`; xóa hết refresh token, tăng token version (Redis), response chỉ có `message` + `loggedOutDevices` (không trả token mới).
- Frontend: Student/Teacher ProfileView gọi `changePassword()` → hiển thị success → sau ~1.5s `authStore.clearAuth()` + `window.location.href = '/login'`.
- Token version: JWT có claim `tv`; `JwtTokenVersionValidator` so sánh với Redis; đổi mật khẩu gọi `incrementTokenVersion(userId)` để vô hiệu hóa mọi access token cũ.

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1312 | Current password is incorrect | 400 |
| 1310 | Passwords do not match | 400 |
| 1313 | New password must be different | 400 |
| 1122 | Password too weak | 400 |

---

### UC-11: Quản Lý Users (Admin) — Chi Tiết ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/users` |
| **Actor** | Admin |
| **Mục đích** | CRUD tài khoản (Teacher/Student), quản lý trạng thái, reset password |
| **Authorization** | `Bearer {token}` với `role = ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`) |

> **⚠️ Quy tắc quan trọng:**
> - Tài khoản **ADMIN** là mặc định (seeded), **KHÔNG** tạo thêm qua API.
> - Admin **KHÔNG** thể tự xóa chính mình.
> - Khi tạo user, admin chọn role (`TEACHER` hoặc `STUDENT`) → form hiển thị fields tương ứng.
> - Tất cả xóa đều là **soft delete** (`deleted_at = NOW()`).

---

#### UC-11.1: Danh Sách Users ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /admin/users` |
| **Actor** | Admin |
| **Mục đích** | Xem danh sách users với phân trang, filter, search |

**Query Params:**

| Param | Type | Default | Mô tả |
|-------|------|---------|-------|
| `page` | int | 0 | Trang hiện tại |
| `size` | int | 20 | Số record/trang (max 100) |
| `sort` | string | `createdAt,desc` | Sắp xếp |
| `search` | string | | Tìm theo email (LIKE, case-insensitive) |
| `status` | string | | Filter: `ACTIVE`, `INACTIVE`, `BLOCKED`, `PENDING_VERIFICATION` |
| `roleId` | int | | Filter: 1=ADMIN, 2=TEACHER, 3=STUDENT |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘
    │                   │                       │
    │ 1. GET /admin/users?search=&status=&roleId=
    │──────────────────►│                       │
    │                   │                       │
    │                   │ 2. Normalize search   │
    │                   │    (lowercase, escape %, _)
    │                   │ 3. Query with filters │
    │                   │──────────────────────►│
    │                   │◄──────────────────────│
    │                   │                       │
    │                   │ 4. For each user:     │
    │                   │    - Resolve fullName from Teacher/Student profile
    │                   │    - Resolve avatar URL via StorageService
    │                   │──────────────────────►│
    │                   │◄──────────────────────│
    │                   │                       │
    │ 5. Return paginated list                  │
    │◄──────────────────│                       │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "userId": "uuid-001",
        "email": "admin@fpt.edu.vn",
        "fullName": "Admin User",
        "role": { "roleId": 1, "roleName": "ADMIN" },
        "status": "ACTIVE",
        "emailVerified": true,
        "profilePictureUrl": "http://localhost:9000/student-management/avatar/...",
        "lastLoginAt": "2026-02-10T08:00:00",
        "loginCount": 42,
        "createdAt": "2025-09-01T08:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 22,
    "totalPages": 2
  }
}
```

> **Implementation notes:**
> - `fullName` được resolve từ `teachers`/`students` table (firstName + lastName), fallback build từ email nếu chưa có profile.
> - `profilePictureUrl` trả full URL qua `IStorageService.getPublicUrl()`.
> - Search dùng `LOWER(email) LIKE :search` với `%keyword%`.

---

#### UC-11.2: Xem Chi Tiết User ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /admin/users/{userId}` |
| **Actor** | Admin |
| **Mục đích** | Xem thông tin user + profile (student/teacher) |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘
    │                   │                       │
    │ 1. GET /admin/users/{userId}              │
    │──────────────────►│                       │
    │                   │                       │
    │                   │ 2. Find user (deletedAt IS NULL)
    │                   │──────────────────────►│
    │                   │◄──────────────────────│
    │                   │                       │
    │                   │ 3. Switch by role:    │
    │                   │    IF TEACHER → fetch teacher + department
    │                   │    IF STUDENT → fetch student + department
    │                   │──────────────────────►│
    │                   │◄──────────────────────│
    │                   │                       │
    │ 4. Return user + profile                  │
    │◄──────────────────│                       │
```

**Response (Student):**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-201",
    "email": "hoang.minh.tuan@fpt.edu.vn",
    "role": { "roleId": 3, "roleName": "STUDENT" },
    "status": "ACTIVE",
    "emailVerified": true,
    "banReason": null,
    "lastLoginAt": "2026-02-10T07:30:00",
    "loginCount": 45,
    "createdAt": "2025-09-01T08:00:00",
    "studentProfile": {
      "studentId": "uuid-2001",
      "studentCode": "HE170001",
      "firstName": "Tuan",
      "lastName": "Hoang Minh",
      "dob": "2003-03-15",
      "gender": "MALE",
      "major": "Software Engineering",
      "email": "hoang.minh.tuan@fpt.edu.vn",
      "phone": "0912000001",
      "address": "12 Tran Phu, Ha Noi",
      "gpa": 3.45,
      "year": 2,
      "manageClass": "SE1701",
      "department": { "departmentId": 1, "name": "Computer Science" }
    },
    "teacherProfile": null
  }
}
```

**Response (Teacher):**
```json
{
  "code": 1000,
  "result": {
    "userId": "uuid-101",
    "email": "nguyen.van.an@fpt.edu.vn",
    "role": { "roleId": 2, "roleName": "TEACHER" },
    "status": "ACTIVE",
    "emailVerified": true,
    "banReason": null,
    "lastLoginAt": "2026-02-10T08:00:00",
    "loginCount": 30,
    "createdAt": "2025-06-01T08:00:00",
    "studentProfile": null,
    "teacherProfile": {
      "teacherId": "uuid-1001",
      "teacherCode": "HJ170001",
      "firstName": "An",
      "lastName": "Nguyen Van",
      "email": "nguyen.van.an@fpt.edu.vn",
      "phone": "0901000001",
      "specialization": "Artificial Intelligence",
      "academicRank": "Associate Professor",
      "officeRoom": "A-201",
      "degreesQualification": "PhD Computer Science",
      "department": { "departmentId": 1, "name": "Computer Science" }
    }
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |

---

#### UC-11.3a: Tạo User (Teacher hoặc Student) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /admin/users` |
| **Actor** | Admin |
| **Mục đích** | Tạo tài khoản mới + profile (Teacher/Student) |
| **Postcondition** | User tạo xong có `status = PENDING_VERIFICATION`, gửi welcome email |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐      ┌───────┐      ┌────────────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │      │ Redis │      │ Email SMTP │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘      └───┬───┘      └─────┬──────┘
    │                   │                       │                │                │
    │ 1. POST /admin/users                      │                │                │
    │    {role, email, profile fields}           │                │                │
    │──────────────────►│                       │                │                │
    │                   │                       │                │                │
    │                   │ 2. Validate role (TEACHER/STUDENT only)│                │
    │                   │ 3. Check email unique │                │                │
    │                   │ 4. Check code unique  │                │                │
    │                   │    (teacherCode/studentCode)           │                │
    │                   │ 5. Find department    │                │                │
    │                   │──────────────────────►│                │                │
    │                   │◄──────────────────────│                │                │
    │                   │                       │                │                │
    │                   │ 6. Generate random password (12 chars) │                │
    │                   │ 7. Hash password (BCrypt)              │                │
    │                   │ 8. Create users record │                │                │
    │                   │    (PENDING_VERIFICATION)              │                │
    │                   │ 9. Create profile:    │                │                │
    │                   │    teachers/students  │                │                │
    │                   │──────────────────────►│                │                │
    │                   │                       │                │                │
    │                   │ 10. Create activation token (TTL: 72h) │                │
    │                   │────────────────────────────────────────►│                │
    │                   │                       │                │                │
    │                   │ 11. Send welcome email│                │                │
    │                   │    (email + password + activation link)│                │
    │                   │──────────────────────────────────────────────────────────►│
    │                   │                       │                │                │
    │ 12. Return user + profile (201 CREATED)   │                │                │
    │◄──────────────────│                       │                │                │
```

**Request (role = TEACHER):**
```json
{
  "role": "TEACHER",
  "email": "nguyen.thi.hoa@fpt.edu.vn",
  "departmentId": 1,
  "teacherCode": "HJ170006",
  "firstName": "Hoa",
  "lastName": "Nguyen Thi",
  "phone": "0901000006",
  "specialization": "Cloud Computing",
  "academicRank": "Lecturer",
  "officeRoom": "A-301",
  "degreesQualification": "PhD"
}
```

**Request (role = STUDENT):**
```json
{
  "role": "STUDENT",
  "email": "tran.thi.anh@fpt.edu.vn",
  "departmentId": 1,
  "studentCode": "HE170016",
  "firstName": "Anh",
  "lastName": "Tran Thi",
  "dob": "2003-05-20",
  "gender": "FEMALE",
  "major": "Software Engineering",
  "phone": "0912000016",
  "address": "100 Le Loi, HCM",
  "year": 2,
  "manageClass": "SE1701"
}
```

**Validation Rules:**

| Field | Rule | Áp dụng |
|-------|------|---------|
| role | Required, `TEACHER` hoặc `STUDENT` | Cả hai |
| email | Required, `@fpt.edu.vn`, unique, max 100 | Cả hai |
| departmentId | Required, must exist | Cả hai |
| firstName | Required, max 50 | Cả hai |
| lastName | Required, max 50 | Cả hai |
| phone | Optional, digits only, max 20 | Cả hai |
| teacherCode | Required (TEACHER), format `HJxxxxxx`, unique | Teacher |
| specialization | Optional, max 100 | Teacher |
| academicRank | Optional, max 50 | Teacher |
| officeRoom | Optional, max 50 | Teacher |
| degreesQualification | Optional, TEXT | Teacher |
| studentCode | Required (STUDENT), format `HExxxxxx`, unique | Student |
| dob | Optional, must be in the past | Student |
| gender | Optional, `MALE`/`FEMALE`/`OTHER` | Student |
| major | Optional, max 100 | Student |
| address | Optional, max 255 | Student |
| year | Optional, 1-4 | Student |
| manageClass | Optional, max 50 | Student |

**Password Generation:**
```
Thuật toán: SecureRandom, 12 ký tự
  - Ít nhất 1 chữ hoa (A-Z, loại O, I, L)
  - Ít nhất 1 chữ thường (a-z, loại o, l)
  - Ít nhất 1 chữ số (2-9, loại 0, 1)
  - Ít nhất 1 ký tự đặc biệt (!@#$%&*)
  - Shuffle toàn bộ để tránh pattern
```

**Welcome Email:**
```
Subject: [Student Management] Tài khoản của bạn đã được tạo

Xin chào {firstName} {lastName},

Tài khoản đã được tạo:
  Email:    nguyen.thi.hoa@fpt.edu.vn
  Mật khẩu: Abc@12345xyz

Click link để kích hoạt (hết hạn sau 72 giờ):
  https://domain/auth/activate?token=xxx
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1200 | User already exists | 409 |
| 1100 | Email is required | 400 |
| 1101 | Invalid email format | 400 |
| 1210 | Invalid role (must be TEACHER or STUDENT) | 400 |
| 1203 | Teacher code already exists | 409 |
| 1204 | Student code already exists | 409 |
| 1220 | Department not found | 400 |
| 1000 | Validation error (first/last name required, etc.) | 400 |

---

#### UC-11.3b: Import Users từ Excel (Planned)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /admin/users/import` |
| **Content-Type** | `multipart/form-data` |
| **Actor** | Admin |
| **Mục đích** | Import hàng loạt Teacher/Student từ file Excel |

> **Template:** Hệ thống tự detect loại template dựa trên header row.
> - `GET /admin/users/import/template?role=TEACHER` → Download Template Teacher
> - `GET /admin/users/import/template?role=STUDENT` → Download Template Student

**Logic mỗi row:**
1. Validate fields + check unique (email, code)
2. Lookup `departmentName` → `departmentId`
3. Generate random password
4. Create `users` + profile record (`PENDING_VERIFICATION`)
5. Gửi welcome email
6. Row lỗi → skip, ghi vào `failures`

**Business Rules:** File max **10 MB**, max **500 rows**, partial import (row lỗi bị skip).

---

#### UC-11.4: Cập Nhật Profile User (Admin) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PUT /admin/users/{userId}/profile` |
| **Actor** | Admin |
| **Mục đích** | Admin cập nhật profile teacher/student |
| **Đặc điểm** | Partial update, KHÔNG cho sửa: email, password, status, role |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘
    │                   │                       │
    │ 1. PUT /admin/users/{userId}/profile      │
    │──────────────────►│                       │
    │                   │                       │
    │                   │ 2. Find user          │
    │                   │ 3. Detect role (TEACHER/STUDENT)
    │                   │ 4. Find department if departmentId provided
    │                   │──────────────────────►│
    │                   │◄──────────────────────│
    │                   │                       │
    │                   │ 5. IF TEACHER:        │
    │                   │    - Find teacher profile
    │                   │    - Update fields (partial)
    │                   │    - Check teacherCode unique if changed
    │                   │    IF STUDENT:        │
    │                   │    - Find student profile
    │                   │    - Update fields (partial)
    │                   │    - Check studentCode unique if changed
    │                   │ 6. Save profile       │
    │                   │──────────────────────►│
    │                   │                       │
    │ 7. Return updated user + profile          │
    │◄──────────────────│                       │
```

**Request:**
```json
{
  "firstName": "Hoa Updated",
  "lastName": "Nguyen Thi",
  "phone": "0901999999",
  "departmentId": 2,
  "teacherCode": "HJ170006",
  "specialization": "DevOps",
  "academicRank": "Senior Lecturer"
}
```

> **Lưu ý:** Chỉ gửi các field cần update. Field null/absent sẽ bị bỏ qua (partial update).

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |
| 1210 | Invalid role | 400 |
| 1220 | Department not found | 400 |
| 1203 | Teacher code already exists | 409 |
| 1204 | Student code already exists | 409 |
| 1501 | Student profile not found | 404 |
| 1502 | Teacher profile not found | 404 |

---

#### UC-11.5: Thay Đổi Trạng Thái User (Block/Unblock) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `PATCH /admin/users/{userId}/status` |
| **Actor** | Admin |
| **Mục đích** | Thay đổi trạng thái user giữa ACTIVE / INACTIVE / BLOCKED |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐      ┌───────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │      │ Redis │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘      └───┬───┘
    │                   │                       │                │
    │ 1. PATCH /admin/users/{id}/status         │                │
    │──────────────────►│                       │                │
    │                   │                       │                │
    │                   │ 2. Find user          │                │
    │                   │──────────────────────►│                │
    │                   │◄──────────────────────│                │
    │                   │                       │                │
    │                   │ 3. Validate transition│                │
    │                   │ 4. Update status      │                │
    │                   │──────────────────────►│                │
    │                   │                       │                │
    │                   │ 5. If BLOCKED/INACTIVE:│                │
    │                   │    → Increment token version           │
    │                   │    → Delete ALL refresh tokens         │
    │                   │    → User bị force logout ngay         │
    │                   │────────────────────────────────────────►│
    │                   │                       │                │
    │ 6. Return updated user + profile          │                │
    │◄──────────────────│                       │                │
```

**Request (Block):**
```json
{
  "status": "BLOCKED",
  "banReason": "Vi phạm nội quy trường"
}
```

**Request (Unblock):**
```json
{
  "status": "ACTIVE"
}
```

**State Machine:**
```
┌─────────────────────┐     ┌─────────────┐
│ PENDING_VERIFICATION│────►│   ACTIVE    │
└─────────────────────┘     └──────┬──────┘
         │                         │  ▲
         │                block ▼  │ unblock
         │                  ┌──────┴──────┐
         └─────────────────►│   BLOCKED   │
                            └──────┬──────┘
                   Admin can also: │
                            ┌──────┴──────┐
                            │  INACTIVE   │
                            └─────────────┘
```

| Transition | Allowed | Side Effect |
|------------|---------|-------------|
| ACTIVE → BLOCKED | ✅ | Force logout (token version + xóa refresh tokens), set banReason |
| BLOCKED → ACTIVE | ✅ | Clear `banReason` |
| ACTIVE → INACTIVE | ✅ | Force logout |
| INACTIVE → ACTIVE | ✅ | — |
| PENDING → ACTIVE | ✅ | Admin override activation |
| PENDING → BLOCKED | ✅ | Force block |
| * → PENDING_VERIFICATION | ❌ | Error: Cannot set back to PENDING |

**Validation:**
- `status` required (`ACTIVE`, `INACTIVE`, `BLOCKED`)
- `banReason` **required** khi `status = BLOCKED` (max 255 chars)
- `banReason` optional khi `status = INACTIVE`
- `banReason` cleared khi `status = ACTIVE`

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |
| 1000 | Status is required / Ban reason required / Cannot set PENDING | 400 |

---

#### UC-11.6: Reset Password (Admin) (Planned)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /admin/users/{userId}/reset-password` |
| **Actor** | Admin |
| **Mục đích** | Admin đặt lại mật khẩu cho user |

**Request:**
```json
{
  "newPassword": "TempPassword123!"
}
```

**Logic:**
1. Hash mật khẩu mới (BCrypt)
2. Update `password_hash` trong DB
3. Increment token version → force logout mọi thiết bị
4. Xóa tất cả refresh tokens

---

#### UC-11.7: Soft Delete User ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `DELETE /admin/users/{userId}` |
| **Actor** | Admin |
| **Mục đích** | Xóa mềm user và profile liên quan |

**Luồng hoạt động:**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐      ┌───────┐
│ Admin │      │ AdminUserService │      │ PostgreSQL │      │ Redis │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘      └───┬───┘
    │                   │                       │                │
    │ 1. DELETE /admin/users/{userId}           │                │
    │──────────────────►│                       │                │
    │                   │                       │                │
    │                   │ 2. Check: admin ≠ target (không tự xóa)
    │                   │ 3. Find user (deletedAt IS NULL)      │
    │                   │──────────────────────►│                │
    │                   │                       │                │
    │                   │ 4. Set user.deletedAt = NOW()          │
    │                   │ 5. Set teacher/student.deletedAt = NOW()
    │                   │──────────────────────►│                │
    │                   │                       │                │
    │                   │ 6. Force logout:      │                │
    │                   │    → Delete ALL refresh tokens         │
    │                   │    → Increment token version           │
    │                   │────────────────────────────────────────►│
    │                   │                       │                │
    │ 7. Return success (204 or 200)            │                │
    │◄──────────────────│                       │                │
```

**Business Rules:**
- Admin **KHÔNG** thể xóa chính mình (`actingAdminId ≠ targetUserId`)
- Soft delete cascade: user + teacher/student profile
- Force logout: increment token version + xóa tất cả refresh tokens

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1201 | User not found | 404 |
| 1000 | Admin cannot delete own account | 400 |

---

## Module: Department

---

### UC-12: Xem Danh Sách Khoa

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /departments` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem danh sách các khoa/bộ môn |

**Luồng hoạt động:**
```
┌─────────┐      ┌────────────────────┐      ┌────────────┐
│  Client │      │ Dept Controller    │      │ PostgreSQL │
└────┬────┘      └──────────┬─────────┘      └──────┬─────┘
     │                     │                     │
     │ 1. GET /departments │                     │
     │────────────────────>│                     │
     │                     │                     │
     │                     │ 2. Fetch all depts  │
     │                     │────────────────────>│
     │                     │<────────────────────│
     │                     │                     │
     │ 3. Return list      │                     │
     │<────────────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "departmentId": 1,
      "name": "Công nghệ Thông tin",
      "officeLocation": "Building A, Room 101"
    },
    {
      "departmentId": 2,
      "name": "Kinh tế",
      "officeLocation": "Building B, Room 201"
    }
  ]
}
```

---

### UC-13: Quản Lý Khoa (Admin) — Chi Tiết

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/departments` |
| **Actor** | Admin |
| **Mục đích** | CRUD khoa/bộ môn |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

#### UC-13.1: Danh Sách Khoa

**Endpoint:** `GET /admin/departments`

**Query Params:** `?page=0&size=20&search=Computer`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "departmentId": 1,
        "name": "Computer Science",
        "officeLocation": "Building A, Room 101",
        "teacherCount": 5,
        "studentCount": 3,
        "createdAt": "2025-09-01T00:00:00"
      }
    ],
    "page": 0, "size": 20, "totalElements": 5, "totalPages": 1
  }
}
```

> **Note:** `teacherCount` và `studentCount` là **computed fields** (`COUNT` từ bảng teachers/students WHERE `department_id = ?` AND `deleted_at IS NULL`).

#### UC-13.2: Tạo Khoa

**Endpoint:** `POST /admin/departments`

**Request:**
```json
{ "name": "Data Science", "officeLocation": "Building F, Room 601" }
```

**Validation:**

| Field | Rule |
|-------|------|
| name | Required, max 100, unique (case-insensitive) |
| officeLocation | Optional, max 100 |

#### UC-13.3: Cập Nhật Khoa

**Endpoint:** `PUT /admin/departments/{departmentId}`

**Request:**
```json
{ "name": "Computer Science & Engineering", "officeLocation": "Building A, Room 102" }
```

#### UC-13.4: Xóa Khoa (Soft Delete)

**Endpoint:** `DELETE /admin/departments/{departmentId}`

**Business Rule:** Không cho xóa nếu còn teacher/student thuộc khoa này (`deleted_at IS NULL`).

| Điều kiện | Kết quả |
|-----------|---------|
| Còn teacher/student active | Error: "Department has active members" |
| Không còn ai | Soft delete OK |

---

## Module: Teacher

---

### UC-14: Xem Danh Sách Giảng Viên

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /teachers` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem danh sách giảng viên |

**Query Params:** `?departmentId=1&page=0&size=20`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "teacherId": 1,
        "firstName": "Nguyen",
        "lastName": "Van A",
        "email": "teacher.a@school.edu",
        "phone": "0901234567",
        "specialization": "Machine Learning",
        "department": {
          "departmentId": 1,
          "name": "Công nghệ Thông tin"
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50
  }
}
```

---

### UC-14.1: Xem Thông Tin Giảng Viên (Teacher Profile)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /teachers/me` |
| **Actor** | Teacher |
| **Mục đích** | Xem thông tin chi tiết hồ sơ giảng viên |
| **Precondition** | User phải có role TEACHER và đã có record trong bảng teachers |

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│ Teacher │      │ Teacher Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. GET /teachers/me                       │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Extract userId from JWT
     │                    │ 3. Check role == TEACHER
     │                    │ 4. Fetch teacher by userId
     │                    │    (JOIN departments)
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 5. Return teacher profile                 │
     │<───────────────────│                      │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "teacherId": 1,
    "userId": 3,
    "firstName": "Nguyen",
    "lastName": "Van A",
    "email": "teacher.a@school.edu",
    "phone": "0901234567",
    "specialization": "Machine Learning",
    "department": {
      "departmentId": 1,
      "name": "Công nghệ Thông tin",
      "officeLocation": "Building A, Room 101"
    },
    "createdAt": "2025-06-01T08:00:00"
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1401 | Access denied - Teacher role required | 403 |
| 1502 | Teacher profile not found | 404 |

---

### UC-15: Quản Lý Giảng Viên (Admin) — Chi Tiết

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/teachers` |
| **Actor** | Admin |
| **Mục đích** | Quản lý danh sách giảng viên, cập nhật, xóa |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

> **Lưu ý:** Tạo giảng viên mới → sử dụng `POST /admin/users` với `role: "TEACHER"` (UC-11.3a).
> Import giảng viên hàng loạt → sử dụng `POST /admin/users/import` với Template Teacher (UC-11.3b).

#### UC-15.1: Danh Sách Giảng Viên

**Endpoint:** `GET /admin/teachers`

**Query Params:**

| Param | Mô tả |
|-------|-------|
| `search` | Tìm theo tên, email, teacherCode |
| `departmentId` | Filter theo khoa |
| `page`, `size`, `sort` | Phân trang |

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "teacherId": "uuid-1001",
        "teacherCode": "HJ170001",
        "firstName": "An",
        "lastName": "Nguyen Van",
        "email": "nguyen.van.an@fpt.edu.vn",
        "phone": "0901000001",
        "specialization": "Artificial Intelligence",
        "academicRank": "Associate Professor",
        "officeRoom": "A-201",
        "department": { "departmentId": 1, "name": "Computer Science" },
        "user": { "userId": "uuid-101", "status": "ACTIVE" },
        "classCount": 4
      }
    ],
    "page": 0, "size": 20, "totalElements": 5, "totalPages": 1
  }
}
```

#### UC-15.3: Cập Nhật Giảng Viên

**Endpoint:** `PUT /admin/teachers/{teacherId}`

Admin có thể update **tất cả field** (bao gồm field mà Teacher tự mình không được sửa):
```json
{
  "departmentId": 2,
  "teacherCode": "HJ170006",
  "firstName": "Hoa",
  "lastName": "Nguyen Thi",
  "phone": "0901000006",
  "specialization": "DevOps",
  "academicRank": "Senior Lecturer",
  "officeRoom": "B-101"
}
```

#### UC-15.4: Xóa Giảng Viên (Soft Delete)

**Endpoint:** `DELETE /admin/teachers/{teacherId}`

**Business Rule:** Không xóa nếu đang dạy lớp trong kỳ hiện tại.

| Điều kiện | Kết quả |
|-----------|---------|
| Đang dạy lớp kỳ hiện tại | Error: "Teacher has active classes" |
| Không dạy lớp nào | Soft delete teacher + soft delete user liên kết |

---

## Module: Student

---

### UC-16: Xem Thông Tin Sinh Viên (Student Profile)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /students/me` |
| **Actor** | Student |
| **Mục đích** | Xem thông tin chi tiết hồ sơ sinh viên |
| **Precondition** | User phải có role STUDENT và đã có record trong bảng students |

**Luồng hoạt động:**
```
┌─────────┐      ┌─────────────────┐      ┌────────────┐
│ Student │      │ Student Service │      │ PostgreSQL │
└────┬────┘      └────────┬────────┘      └──────┬─────┘
     │                    │                      │
     │ 1. GET /students/me                       │
     │ [Authorization: Bearer token]             │
     │───────────────────>│                      │
     │                    │                      │
     │                    │ 2. Extract userId from JWT
     │                    │ 3. Check role == STUDENT
     │                    │ 4. Fetch student by userId
     │                    │    (JOIN departments)
     │                    │─────────────────────>│
     │                    │<─────────────────────│
     │                    │                      │
     │ 5. Return student profile                 │
     │<───────────────────│                      │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "studentId": 1,
    "userId": 5,
    "firstName": "Tran",
    "lastName": "Van C",
    "dob": "2000-05-15",
    "email": "student.c@school.edu",
    "phone": "0909876543",
    "address": "123 ABC Street, HCM City",
    "department": {
      "departmentId": 1,
      "name": "Công nghệ Thông tin",
      "officeLocation": "Building A, Room 101"
    },
    "createdAt": "2026-01-15T10:00:00"
  }
}
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1401 | Access denied - Student role required | 403 |
| 1501 | Student profile not found | 404 |

---

### UC-17: Quản Lý Sinh Viên (Admin) — Chi Tiết

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/students` |
| **Actor** | Admin |
| **Mục đích** | Quản lý danh sách sinh viên, cập nhật, xóa |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

> **Lưu ý:** Tạo sinh viên mới → sử dụng `POST /admin/users` với `role: "STUDENT"` (UC-11.3a).
> Import sinh viên hàng loạt → sử dụng `POST /admin/users/import` với Template Student (UC-11.3b).

#### UC-17.1: Danh Sách Sinh Viên

**Endpoint:** `GET /admin/students`

**Query Params:**

| Param | Mô tả |
|-------|-------|
| `search` | Tìm theo tên, email, studentCode |
| `departmentId` | Filter theo khoa |
| `major` | Filter theo chuyên ngành |
| `gender` | Filter: MALE, FEMALE |
| `page`, `size`, `sort` | Phân trang |

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "studentId": "uuid-2001",
        "studentCode": "HE170001",
        "firstName": "Tuan",
        "lastName": "Hoang Minh",
        "dob": "2003-03-15",
        "gender": "MALE",
        "major": "Software Engineering",
        "email": "hoang.minh.tuan@fpt.edu.vn",
        "phone": "0912000001",
        "address": "12 Tran Phu, Ha Noi",
        "gpa": 3.45,
        "year": 2,
        "manageClass": "SE1701",
        "department": { "departmentId": 1, "name": "Computer Science" },
        "user": { "userId": "uuid-201", "status": "ACTIVE" },
        "enrollmentCount": 4
      }
    ],
    "page": 0, "size": 20, "totalElements": 15, "totalPages": 1
  }
}
```

#### UC-17.3: Cập Nhật Sinh Viên

**Endpoint:** `PUT /admin/students/{studentId}`

Admin update tất cả field (trừ `gpa` — hệ thống tự tính):
```json
{
  "departmentId": 2,
  "studentCode": "HE170016",
  "firstName": "Anh",
  "lastName": "Tran Thi",
  "dob": "2003-05-20",
  "gender": "FEMALE",
  "major": "Data Science",
  "phone": "0912000016",
  "address": "200 Nguyen Hue, HCM",
  "year": 3,
  "manageClass": "DS1801"
}
```

#### UC-17.4: Xóa Sinh Viên (Soft Delete)

**Endpoint:** `DELETE /admin/students/{studentId}`

**Business Rule:** Không xóa nếu đang có enrollment trong kỳ hiện tại.

---

## Module: Course

---

### UC-18: Xem Danh Sách Môn Học

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /courses` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem danh sách môn học |

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "courseId": 1,
      "name": "Lập trình Java",
      "credits": 3,
      "description": "Môn học về lập trình Java cơ bản đến nâng cao"
    }
  ]
}
```

---

### UC-19: Quản Lý Môn Học (Admin) — Chi Tiết

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/courses` |
| **Actor** | Admin |
| **Mục đích** | CRUD môn học |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

#### UC-19.1: Danh Sách Môn Học

**Endpoint:** `GET /admin/courses`

**Query Params:** `?search=Programming&page=0&size=20`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "courseId": 1,
        "name": "Introduction to Programming",
        "credits": 3,
        "description": "Fundamentals of programming using Java...",
        "classCount": 2,
        "createdAt": "2025-09-01T00:00:00"
      }
    ],
    "page": 0, "size": 20, "totalElements": 10, "totalPages": 1
  }
}
```

#### UC-19.2: Tạo Môn Học

**Endpoint:** `POST /admin/courses`

**Request:**
```json
{
  "name": "Machine Learning",
  "credits": 4,
  "description": "Introduction to ML algorithms, supervised and unsupervised learning."
}
```

**Validation:**

| Field | Rule |
|-------|------|
| name | Required, max 100, unique |
| credits | Required, integer > 0 |
| description | Optional, TEXT |

#### UC-19.3: Cập Nhật Môn Học

**Endpoint:** `PUT /admin/courses/{courseId}`

#### UC-19.4: Xóa Môn Học (Soft Delete)

**Endpoint:** `DELETE /admin/courses/{courseId}`

**Business Rule:** Không xóa nếu có scheduled_classes đang sử dụng.

---

## Module: Scheduled Class

---

### UC-20: Xem Lớp Học Phần

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /classes` |
| **Actor** | Authenticated User |
| **Mục đích** | Xem danh sách lớp học phần |

**Query Params:** `?semester=1&year=2026&courseId=1`

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "classId": 1,
      "course": {
        "courseId": 1,
        "name": "Lập trình Java"
      },
      "teacher": {
        "teacherId": 1,
        "firstName": "Nguyen",
        "lastName": "Van A"
      },
      "semester": "1",
      "year": 2026,
      "roomNumber": "A101",
      "schedule": "Mon 8:00-10:00"
    }
  ]
}
```

---

### UC-21: Quản Lý Lớp Học Phần (Admin) — Chi Tiết

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/classes` |
| **Actor** | Admin |
| **Mục đích** | CRUD lớp học phần |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

#### UC-21.1: Danh Sách Lớp Học Phần

**Endpoint:** `GET /admin/classes`

**Query Params:**

| Param | Mô tả |
|-------|-------|
| `semesterName` | Filter: SPRING/SUMMER/FALL |
| `year` | Filter năm |
| `courseId` | Filter theo môn |
| `teacherId` | Filter theo giảng viên |
| `page`, `size`, `sort` | Phân trang |

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "classId": 7,
        "course": { "courseId": 2, "name": "Data Structures and Algorithms", "credits": 4 },
        "teacher": {
          "teacherId": "uuid-1001",
          "teacherCode": "HJ170001",
          "firstName": "An", "lastName": "Nguyen Van"
        },
        "semester": "SPRING",
        "year": 2026,
        "roomNumber": "A-102",
        "schedule": "Mon 10:00-12:00",
        "enrollmentCount": 3,
        "createdAt": "2025-12-01T00:00:00"
      }
    ],
    "page": 0, "size": 20, "totalElements": 15, "totalPages": 1
  }
}
```

#### UC-21.2: Tạo Lớp Học Phần

**Endpoint:** `POST /admin/classes`

**Request:**
```json
{
  "courseId": 2,
  "teacherId": "uuid-1001",
  "semester": "SUMMER",
  "year": 2026,
  "roomNumber": "A-201",
  "schedule": "Mon 08:00-10:00, Wed 08:00-10:00"
}
```

**Validation:**

| Field | Rule |
|-------|------|
| courseId | Required, must exist |
| teacherId | Optional (có thể chưa assign), must exist nếu có |
| semester | Required, enum: SPRING/SUMMER/FALL |
| year | Required, integer |
| roomNumber | Optional, max 20 |
| schedule | Optional, max 50 |

**Business Rule:** Kiểm tra conflict lịch dạy của teacher (cùng semester+year+schedule).

#### UC-21.3: Cập Nhật Lớp Học Phần

**Endpoint:** `PUT /admin/classes/{classId}`

#### UC-21.4: Xóa Lớp Học Phần (Soft Delete)

**Endpoint:** `DELETE /admin/classes/{classId}`

**Business Rule:** Không xóa nếu đã có enrollment + grade.

---

## Module: Enrollment

---

### UC-22: Đăng Ký Lớp Học Phần

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /enrollments` |
| **Actor** | Student |
| **Mục đích** | Đăng ký lớp học phần |

**Request Body:**
```json
{
  "classId": 1
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌────────────────────┐      ┌────────────┐
│ Student │      │ Enrollment Service │      │ PostgreSQL │
└────┬────┘      └─────────┬──────────┘      └──────┬─────┘
     │                     │                        │
     │ 1. POST /enrollments                         │
     │────────────────────>│                        │
     │                     │                        │
     │                     │ 2. Check class exists  │
     │                     │ 3. Check not already enrolled
     │                     │ 4. Check class capacity│
     │                     │───────────────────────>│
     │                     │<───────────────────────│
     │                     │                        │
     │                     │ 5. Create enrollment   │
     │                     │───────────────────────>│
     │                     │                        │
     │ 6. Enrollment success                        │
     │<────────────────────│                        │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "enrollmentId": 1,
    "studentId": 1,
    "classId": 1,
    "enrollmentDate": "2026-02-02",
    "message": "Enrolled successfully"
  }
}
```

---

### UC-23: Xem Lớp Đã Đăng Ký

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /enrollments/me` |
| **Actor** | Student |
| **Mục đích** | Xem danh sách lớp đã đăng ký |

**Luồng hoạt động:**
```
┌─────────┐      ┌────────────────────┐      ┌────────────┐
│ Student │      │ Enrollment Service │      │ PostgreSQL │
└────┬────┘      └──────────┬─────────┘      └──────┬─────┘
     │                     │                     │
     │ 1. GET /enrollments/me                    │
     │────────────────────>│                     │
     │                     │                     │
     │                     │ 2. Get student from JWT
     │                     │ 3. Fetch enrollments with class, course, grade
     │                     │────────────────────>│
     │                     │<────────────────────│
     │                     │                     │
     │ 4. Return enrollment list                 │
     │<────────────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "enrollmentId": 1,
      "class": {
        "classId": 1,
        "course": { "name": "Lập trình Java" },
        "teacher": { "firstName": "Nguyen", "lastName": "Van A" },
        "schedule": "Mon 8:00-10:00",
        "roomNumber": "A101"
      },
      "enrollmentDate": "2026-02-02",
      "grade": null
    }
  ]
}
```

---

### UC-24: Hủy Đăng Ký Lớp

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `DELETE /enrollments/{enrollmentId}` |
| **Actor** | Student |
| **Mục đích** | Hủy đăng ký lớp học phần |

**Luồng hoạt động:**
```
┌─────────┐      ┌────────────────────┐      ┌────────────┐
│ Student │      │ Enrollment Service │      │ PostgreSQL │
└────┬────┘      └──────────┬─────────┘      └──────┬─────┘
     │                     │                     │
     │ 1. DELETE /enrollments/{id}               │
     │────────────────────>│                     │
     │                     │                     │
     │                     │ 2. Verify ownership (student owns this enrollment)
     │                     │ 3. Check if can cancel (deadline, grade exists?)
     │                     │────────────────────>│
     │                     │<────────────────────│
     │                     │                     │
     │                     │ 4. Delete enrollment│
     │                     │────────────────────>│
     │                     │                     │
     │ 5. Success          │                     │
     │<────────────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "message": "Enrollment cancelled successfully"
  }
}
```

---

## Module: Grade

---

### UC-25: Nhập Điểm (Teacher)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /grades` |
| **Actor** | Teacher |
| **Mục đích** | Nhập điểm cho sinh viên |

**Request Body:**
```json
{
  "enrollmentId": 1,
  "gradeValue": 8.5,
  "feedback": "Làm bài tốt, cần cải thiện phần thực hành"
}
```

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐
│ Teacher │      │ Grade Service│      │ PostgreSQL │
└────┬────┘      └─────┬────────┘      └──────┬─────┘
     │               │                     │
     │ 1. POST /grades                      │
     │──────────────>│                     │
     │               │                     │
     │               │ 2. Verify teacher teaches this class
     │               │ 3. Get enrollment    │
     │               │────────────────────>│
     │               │<────────────────────│
     │               │                     │
     │               │ 4. Create/Update grade
     │               │────────────────────>│
     │               │                     │
     │ 5. Return grade                      │
     │<──────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "gradeId": 1,
    "enrollmentId": 1,
    "gradeValue": 8.5,
    "feedback": "Làm bài tốt, cần cải thiện phần thực hành"
  }
}
```

---

### UC-26: Xem Điểm Của Lớp (Teacher)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /classes/{classId}/grades` |
| **Actor** | Teacher |
| **Mục đích** | Xem điểm tất cả sinh viên trong lớp |

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐
│ Teacher │      │ Grade Service│      │ PostgreSQL │
└────┬────┘      └─────┬────────┘      └──────┬─────┘
     │               │                     │
     │ 1. GET /classes/{id}/grades          │
     │──────────────>│                     │
     │               │                     │
     │               │ 2. Verify teacher owns this class
     │               │ 3. Fetch all enrollments with students and grades
     │               │────────────────────>│
     │               │<────────────────────│
     │               │                     │
     │ 4. Return grade list                 │
     │<──────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "student": {
        "studentId": 1,
        "firstName": "Tran",
        "lastName": "Van C"
      },
      "gradeValue": 8.5,
      "feedback": "Làm bài tốt"
    }
  ]
}
```

---

### UC-27: Xem Điểm Của Mình (Student)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /grades/me` |
| **Actor** | Student |
| **Mục đích** | Xem bảng điểm cá nhân |

**Luồng hoạt động:**
```
┌─────────┐      ┌──────────────┐      ┌────────────┐
│ Student │      │ Grade Service│      │ PostgreSQL │
└────┬────┘      └─────┬────────┘      └──────┬─────┘
     │               │                     │
     │ 1. GET /grades/me                    │
     │──────────────>│                     │
     │               │                     │
     │               │ 2. Get student from JWT
     │               │ 3. Fetch all grades with course info
     │               │────────────────────>│
     │               │<────────────────────│
     │               │                     │
     │ 4. Return transcript                 │
     │<──────────────│                     │
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "course": {
        "courseId": 1,
        "name": "Lập trình Java",
        "credits": 3
      },
      "semester": "1",
      "year": 2026,
      "gradeValue": 8.5,
      "feedback": "Làm bài tốt"
    }
  ]
}
```

---

## Module: Admin - Semester, Dashboard & System Health

---

### UC-28: Quản Lý Học Kỳ (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/semesters` |
| **Actor** | Admin |
| **Mục đích** | CRUD học kỳ, đặt kỳ hiện tại |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

> **Lưu ý:** Bảng `semesters` là cần thiết để Admin quản lý semester khi tạo lớp học phần.

#### UC-28.1: Danh Sách Học Kỳ

**Endpoint:** `GET /admin/semesters`

**Query Params:** `?year=2026&page=0&size=20&sort=year,desc`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "semesterId": 1,
        "name": "SPRING",
        "year": 2026,
        "displayName": "Spring 2026",
        "startDate": "2026-01-12",
        "endDate": "2026-05-10",
        "isCurrent": true,
        "classCount": 9
      }
    ],
    "page": 0, "size": 20, "totalElements": 6, "totalPages": 1
  }
}
```

#### UC-28.2: Tạo Học Kỳ

**Endpoint:** `POST /admin/semesters`

**Request:**
```json
{
  "name": "FALL",
  "year": 2026,
  "startDate": "2026-09-01",
  "endDate": "2026-12-20"
}
```

**Validation:**

| Field | Rule |
|-------|------|
| name | Required, enum: SPRING/SUMMER/FALL |
| year | Required, integer |
| (name, year) | Unique constraint |
| startDate | Required, DATE |
| endDate | Required, DATE, phải sau startDate |

#### UC-28.3: Đặt Kỳ Hiện Tại

**Endpoint:** `PATCH /admin/semesters/{semesterId}/set-current`

**Logic:**
1. Set tất cả semester khác `is_current = FALSE`
2. Set semester này `is_current = TRUE`
3. Đảm bảo chỉ 1 semester `is_current` (partial unique index)

---

### UC-29: Dashboard Thống Kê (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `GET /admin/dashboard` |
| **Actor** | Admin |
| **Mục đích** | Tổng quan nhanh cho trang Admin Dashboard |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

**Response:**
```json
{
  "code": 1000,
  "result": {
    "totalUsers": 22,
    "usersByRole": {
      "ADMIN": 1,
      "TEACHER": 5,
      "STUDENT": 15,
      "PENDING_VERIFICATION": 1
    },
    "totalDepartments": 5,
    "totalCourses": 10,
    "currentSemester": {
      "name": "SPRING",
      "year": 2026,
      "displayName": "Spring 2026"
    },
    "currentSemesterStats": {
      "totalClasses": 9,
      "totalEnrollments": 24,
      "averageClassSize": 2.7
    }
  }
}
```

---

### UC-30: System Health Monitoring (Admin) ✅ IMPLEMENTED

| Thuộc tính | Giá trị |
|------------|---------|
| **Base URL** | `/admin/system/health` |
| **Actor** | Admin |
| **Mục đích** | Kiểm tra trạng thái các thành phần hệ thống |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` |

#### UC-30.1: Tổng Quan Hệ Thống

**Endpoint:** `GET /admin/system/health/overall`

**Response:**
```json
{
  "code": 1000,
  "result": {
    "overallStatus": "UP",
    "timestamp": "2026-02-10T08:00:00Z",
    "api": { "status": "UP", "message": "API is running", "latencyMs": 2 },
    "database": { "status": "UP", "message": "PostgreSQL connected", "latencyMs": 5 },
    "redis": { "status": "UP", "message": "Redis connected", "latencyMs": 1 },
    "mail": { "status": "UP", "message": "SMTP connected", "latencyMs": 120 },
    "minio": { "status": "UP", "message": "MinIO connected", "latencyMs": 8 },
    "nginx": { "status": "UP", "message": "Nginx responding", "latencyMs": 3 },
    "frontend": { "status": "UP", "message": "Frontend accessible", "latencyMs": 15 }
  }
}
```

#### Các Endpoints Health Riêng Lẻ

| Endpoint | Mô tả |
|----------|-------|
| `GET /admin/system/health/api` | Kiểm tra API server |
| `GET /admin/system/health/db` | Kiểm tra PostgreSQL |
| `GET /admin/system/health/redis` | Kiểm tra Redis |
| `GET /admin/system/health/mail` | Kiểm tra SMTP |
| `GET /admin/system/health/minio` | Kiểm tra MinIO storage |
| `GET /admin/system/health/nginx` | Kiểm tra Nginx reverse proxy |
| `GET /admin/system/health/frontend` | Kiểm tra Frontend SPA |

#### Legacy Endpoint (FE compatibility)

**Endpoint:** `GET /admin/health` — Adapter giữ compatible với Frontend hiện tại.

---

## Bảng Tổng Hợp Phân Quyền

| UC | Endpoint | Guest | Student | Teacher | Admin |
|----|----------|-------|---------|---------|-------|
| UC-01 | POST /auth/login | ✅ | - | - | - |
| UC-02 | POST /auth/register | ✅ | - | - | - |
| UC-03 | GET /auth/verify-email | ✅ | - | - | - |
| UC-03.1 | POST /auth/resend-verification | ✅ | - | - | - |
| UC-04 | POST /auth/refresh-token | - | ✅ | ✅ | ✅ |
| UC-05 | POST /auth/logout | - | ✅ | ✅ | ✅ |
| UC-06 | POST /auth/forgot-password | ✅ | - | - | - |
| UC-07 | POST /auth/reset-password | ✅ | - | - | - |
| UC-08 | GET /users/me | - | ✅ | ✅ | ✅ |
| UC-09 | PUT /users/me | - | ✅ | ✅ | ✅ |
| UC-09.1 | PUT /students/me | - | ✅ | - | - |
| UC-09.2 | PUT /teachers/me | - | - | ✅ | - |
| UC-09.3 | POST /profile/me/avatar | - | ✅ | ✅ | ✅ |
| UC-10 | POST /users/me/change-password | - | ✅ | ✅ | ✅ |
| **UC-11.1** | **GET /admin/users** | - | - | - | ✅ |
| **UC-11.2** | **GET /admin/users/{id}** | - | - | - | ✅ |
| **UC-11.3a** | **POST /admin/users** | - | - | - | ✅ |
| **UC-11.3b** | **POST /admin/users/import** | - | - | - | ✅ |
| **UC-11.4** | **PUT /admin/users/{id}/profile** | - | - | - | ✅ |
| **UC-11.5** | **PATCH /admin/users/{id}/status** | - | - | - | ✅ |
| **UC-11.6** | **POST /admin/users/{id}/reset-password** | - | - | - | ✅ |
| **UC-11.7** | **DELETE /admin/users/{id}** | - | - | - | ✅ |
| UC-12 | GET /departments | - | ✅ | ✅ | ✅ |
| **UC-13.1** | **GET /admin/departments** | - | - | - | ✅ |
| **UC-13.2** | **POST /admin/departments** | - | - | - | ✅ |
| **UC-13.3** | **PUT /admin/departments/{id}** | - | - | - | ✅ |
| **UC-13.4** | **DELETE /admin/departments/{id}** | - | - | - | ✅ |
| UC-14 | GET /teachers | - | ✅ | ✅ | ✅ |
| UC-14.1 | GET /teachers/me | - | - | ✅ | - |
| **UC-15.1** | **GET /admin/teachers** | - | - | - | ✅ |
| **UC-15.3** | **PUT /admin/teachers/{id}** | - | - | - | ✅ |
| **UC-15.4** | **DELETE /admin/teachers/{id}** | - | - | - | ✅ |
| UC-16 | GET /students/me | - | ✅ | - | - |
| **UC-17.1** | **GET /admin/students** | - | - | - | ✅ |
| **UC-17.3** | **PUT /admin/students/{id}** | - | - | - | ✅ |
| **UC-17.4** | **DELETE /admin/students/{id}** | - | - | - | ✅ |
| UC-18 | GET /courses | - | ✅ | ✅ | ✅ |
| **UC-19.1** | **GET /admin/courses** | - | - | - | ✅ |
| **UC-19.2** | **POST /admin/courses** | - | - | - | ✅ |
| **UC-19.3** | **PUT /admin/courses/{id}** | - | - | - | ✅ |
| **UC-19.4** | **DELETE /admin/courses/{id}** | - | - | - | ✅ |
| UC-20 | GET /classes | - | ✅ | ✅ | ✅ |
| **UC-21.1** | **GET /admin/classes** | - | - | - | ✅ |
| **UC-21.2** | **POST /admin/classes** | - | - | - | ✅ |
| **UC-21.3** | **PUT /admin/classes/{id}** | - | - | - | ✅ |
| **UC-21.4** | **DELETE /admin/classes/{id}** | - | - | - | ✅ |
| UC-22 | POST /enrollments | - | ✅ | - | - |
| UC-23 | GET /enrollments/me | - | ✅ | - | - |
| UC-24 | DELETE /enrollments/{id} | - | ✅ | - | - |
| UC-25 | POST /grades | - | - | ✅ | ✅ |
| UC-26 | GET /classes/{id}/grades | - | - | ✅ | ✅ |
| UC-27 | GET /grades/me | - | ✅ | - | - |
| **UC-28.1** | **GET /admin/semesters** | - | - | - | ✅ |
| **UC-28.2** | **POST /admin/semesters** | - | - | - | ✅ |
| **UC-28.3** | **PATCH /admin/semesters/{id}/set-current** | - | - | - | ✅ |
| **UC-29** | **GET /admin/dashboard** | - | - | - | ✅ |
| **UC-30** | **GET /admin/system/health/*** | - | - | - | ✅ |

