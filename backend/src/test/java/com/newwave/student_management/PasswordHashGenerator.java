package com.newwave.student_management;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility để generate BCrypt hash cho password.
 * Chạy: mvnw test -Dtest=PasswordHashGenerator -pl backend
 */
public class PasswordHashGenerator {

    public static String hashPassword(String rawPassword) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(rawPassword);
    }

    public static void main(String[] args) {
        String password = "ThanhDat003@";
        String hashed = hashPassword(password);

        System.out.println("==============================================");
        System.out.println("Password : " + password);
        System.out.println("BCrypt   : " + hashed);
        System.out.println("==============================================");

        // Verify
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches(password, hashed);
        System.out.println("Verify   : " + matches);
    }
}

