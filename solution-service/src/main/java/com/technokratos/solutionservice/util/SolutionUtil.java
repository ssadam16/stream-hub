package com.technokratos.solutionservice.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class SolutionUtil {
    public String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String normalize(String code) {
        return code
                .replaceAll("//[^\n]*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .replaceAll("#[^\n]*", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
