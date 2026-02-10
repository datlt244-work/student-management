package com.newwave.student_management.infrastructure.storage;

import java.io.InputStream;

public interface IStorageService {

    /**
     * Upload file lên MinIO.
     *
     * @param objectName  path trong bucket (vd: "avatar/userId/uuid.webp")
     * @param inputStream file data
     * @param size        file size in bytes
     * @param contentType MIME type (vd: "image/webp")
     * @return public URL đầy đủ để truy cập file
     */
    String uploadFile(String objectName, InputStream inputStream, long size, String contentType);

    /**
     * Xóa file trên MinIO.
     *
     * @param objectName path trong bucket
     */
    void deleteFile(String objectName);

    /**
     * Trả về URL công khai đầy đủ cho object (dùng khi trả profile/me để frontend hiển thị ảnh sau reload).
     *
     * @param objectName path trong bucket (vd: "avatar/userId/uuid.webp")
     * @return full URL hoặc null nếu objectName null/blank
     */
    String getPublicUrl(String objectName);
}
