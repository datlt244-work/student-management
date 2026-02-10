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

---

### UC-10: Đổi Mật Khẩu (Change Password)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoint** | `POST /users/me/change-password` |
| **Actor** | Authenticated User |
| **Mục đích** | Thay đổi mật khẩu |
| **Security** | Có option logout all other devices |

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
     │                  │ 8. If logoutOtherDevices == true:    │
     │                  │    → Delete ALL refresh tokens       │
     │                  │      (trừ token hiện tại)            │
     │                  │────────────────────────────────────>│
     │                  │                     │                │
     │ 9. Success       │                     │                │
     │<─────────────────│                     │                │
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "message": "Password changed successfully.",
    "loggedOutDevices": 3,
    "newAccessToken": "eyJ...",
    "newRefreshToken": "uuid..."
  }
}
```

**⚠️ Security Logic (Blacklist Approach):**
```
┌─────────────────────────────────────────────────────────────────────────┐
│                  CHANGE PASSWORD - BLACKLIST APPROACH                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Option: logoutOtherDevices (default: true)                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ true:                                                           │    │
│  │   → Delete ALL refresh tokens (trừ current device)              │    │
│  │   → Issue new tokens cho current device                         │    │
│  │   → Devices khác không thể refresh token                        │    │
│  │                                                                 │    │
│  │ false:                                                          │    │
│  │   → Chỉ update password                                         │    │
│  │   → Giữ nguyên sessions của các devices khác                    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  Flow khi logoutOtherDevices = true:                                    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ 1. Update password trong DB                                     │    │
│  │ 2. Get current refresh token từ request body                    │    │
│  │ 3. DEL refresh:token:{userId} (xóa tất cả)                      │    │
│  │ 4. Generate new access + refresh token                          │    │
│  │ 5. Store new refresh token cho current device                   │    │
│  │ 6. Return new tokens + loggedOutDevices count                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│  ⚠️ Note: JWT cũ vẫn valid cho đến khi hết hạn (1h)                    │
│  Nhưng không thể refresh → bắt buộc login lại                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

**Error Responses:**

| Code | Message | HTTP Status |
|------|---------|-------------|
| 1312 | Current password is incorrect | 400 |
| 1310 | Passwords do not match | 400 |
| 1313 | New password must be different | 400 |
| 1122 | Password too weak | 400 |

---

### UC-11: Quản Lý Users (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/users` |
| **Actor** | Admin |
| **Mục đích** | CRUD tài khoản người dùng |

**GET /admin/users** - Danh sách users (có phân trang):
```json
{
  "code": 1000,
  "result": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

**POST /admin/users** - Tạo user mới:
```json
{
  "email": "newuser@example.com",
  "password": "Password123!",
  "roleId": 2,
  "status": "ACTIVE"
}
```

**Luồng hoạt động (Create User):**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐
│ Admin │      │ Admin Controller │      │ PostgreSQL │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘
    │                   │                     │
    │ 1. POST /admin/users                    │
    │ [Role: ADMIN required]                  │
    │──────────────────>│                     │
    │                   │                     │
    │                   │ 2. Validate request │
    │                   │ 3. Check email unique
    │                   │────────────────────>│
    │                   │<────────────────────│
    │                   │                     │
    │                   │ 4. Hash password    │
    │                   │ 5. Create user      │
    │                   │────────────────────>│
    │                   │                     │
    │ 6. Return created user                  │
    │<──────────────────│                     │
```

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

### UC-13: Quản Lý Khoa (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/departments` |
| **Actor** | Admin |
| **Mục đích** | CRUD khoa/bộ môn |

**POST /admin/departments:**
```json
{
  "name": "Khoa Toán học",
  "officeLocation": "Building C, Room 301"
}
```

**Luồng hoạt động (CRUD Pattern - áp dụng cho tất cả Admin CRUD):**
```
┌───────┐      ┌──────────────────┐      ┌────────────┐
│ Admin │      │ Admin Controller │      │ PostgreSQL │
└───┬───┘      └────────┬─────────┘      └──────┬─────┘
    │                   │                     │
    │ [CREATE] POST /admin/{resource}         │
    │ [READ]   GET /admin/{resource}/{id}     │
    │ [UPDATE] PUT /admin/{resource}/{id}     │
    │ [DELETE] DELETE /admin/{resource}/{id}  │
    │──────────────────>│                     │
    │                   │                     │
    │                   │ 1. Check Admin role │
    │                   │ 2. Validate request │
    │                   │ 3. Execute operation│
    │                   │────────────────────>│
    │                   │<────────────────────│
    │                   │                     │
    │ 4. Return result  │                     │
    │<──────────────────│                     │
```

**Note:** Pattern này áp dụng cho tất cả Admin CRUD operations:
- UC-11: /admin/users
- UC-13: /admin/departments  
- UC-15: /admin/teachers
- UC-17: /admin/students
- UC-19: /admin/courses
- UC-21: /admin/classes

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

### UC-15: Quản Lý Giảng Viên (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/teachers` |
| **Actor** | Admin |
| **Mục đích** | CRUD giảng viên |

**POST /admin/teachers:**
```json
{
  "userId": "uuid-of-user",
  "departmentId": 1,
  "firstName": "Nguyen",
  "lastName": "Van B",
  "email": "teacher.b@school.edu",
  "phone": "0901234568",
  "specialization": "Data Science"
}
```

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

### UC-17: Quản Lý Sinh Viên (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/students` |
| **Actor** | Admin |
| **Mục đích** | CRUD sinh viên |

**POST /admin/students:**
```json
{
  "userId": "uuid-of-user",
  "departmentId": 1,
  "firstName": "Le",
  "lastName": "Thi D",
  "dob": "2001-03-20",
  "email": "student.d@school.edu",
  "phone": "0909876544",
  "address": "456 XYZ Street, HCM City"
}
```

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

### UC-19: Quản Lý Môn Học (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/courses` |
| **Actor** | Admin |
| **Mục đích** | CRUD môn học |

**POST /admin/courses:**
```json
{
  "name": "Cơ sở dữ liệu",
  "credits": 4,
  "description": "Môn học về thiết kế và quản lý CSDL"
}
```

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

### UC-21: Quản Lý Lớp Học Phần (Admin)

| Thuộc tính | Giá trị |
|------------|---------|
| **Endpoints** | `GET/POST/PUT/DELETE /admin/classes` |
| **Actor** | Admin |
| **Mục đích** | CRUD lớp học phần |

**POST /admin/classes:**
```json
{
  "courseId": 1,
  "teacherId": 1,
  "semester": "2",
  "year": 2026,
  "roomNumber": "B202",
  "schedule": "Wed 13:00-15:00"
}
```

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
| UC-11 | /admin/users | - | - | - | ✅ |
| UC-12 | GET /departments | - | ✅ | ✅ | ✅ |
| UC-13 | /admin/departments | - | - | - | ✅ |
| UC-14 | GET /teachers | - | ✅ | ✅ | ✅ |
| UC-14.1 | GET /teachers/me | - | - | ✅ | - |
| UC-15 | /admin/teachers | - | - | - | ✅ |
| UC-16 | GET /students/me | - | ✅ | - | - |
| UC-17 | /admin/students | - | - | - | ✅ |
| UC-18 | GET /courses | - | ✅ | ✅ | ✅ |
| UC-19 | /admin/courses | - | - | - | ✅ |
| UC-20 | GET /classes | - | ✅ | ✅ | ✅ |
| UC-21 | /admin/classes | - | - | - | ✅ |
| UC-22 | POST /enrollments | - | ✅ | - | - |
| UC-23 | GET /enrollments/me | - | ✅ | - | - |
| UC-24 | DELETE /enrollments/{id} | - | ✅ | - | - |
| UC-25 | POST /grades | - | - | ✅ | ✅ |
| UC-26 | GET /classes/{id}/grades | - | - | ✅ | ✅ |
| UC-27 | GET /grades/me | - | ✅ | - | - |
