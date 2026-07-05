package com.placesync.common.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class FileValidationService {

    private static final long MAX_RESUME_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;

    public void validatePdf(MultipartFile file) {
        validateNotEmpty(file);
        validateMaxSize(file, MAX_RESUME_BYTES, "PDF");
        validateExtension(file, ".pdf");
        validateMagicBytes(file, new byte[]{0x25, 0x50, 0x44, 0x46}, "PDF");
    }

    public void validateProfileImage(MultipartFile file) {
        validateNotEmpty(file);
        validateMaxSize(file, MAX_IMAGE_BYTES, "image");
        validateImageExtension(file);
        validateImageMagicBytes(file);
    }

    public void validateLogoImage(MultipartFile file) {
        validateNotEmpty(file);
        validateMaxSize(file, MAX_LOGO_BYTES, "logo");
        validateImageExtension(file);
        validateImageMagicBytes(file);
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
    }

    private void validateMaxSize(MultipartFile file, long maxBytes, String type) {
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    type + " file exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + " MB");
        }
    }

    private void validateExtension(MultipartFile file, String expected) {
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(expected)) {
            throw new IllegalArgumentException("Only " + expected + " files are accepted");
        }
    }

    private void validateImageExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) throw new IllegalArgumentException("Filename must not be null");
        String lower = name.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
            throw new IllegalArgumentException("Only JPG and PNG images are accepted");
        }
    }

    private void validateMagicBytes(MultipartFile file, byte[] expected, String type) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(expected.length);
            for (int i = 0; i < expected.length; i++) {
                if (i >= header.length || header[i] != expected[i]) {
                    throw new IllegalArgumentException("File content does not match expected " + type + " format");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file content for validation");
        }
    }

    private void validateImageMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(8);
            if (isJpeg(header) || isPng(header)) return;
            throw new IllegalArgumentException("File content does not match expected JPEG or PNG format");
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file content for validation");
        }
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] header) {
        return header.length >= 4
                && (header[0] & 0xFF) == 0x89
                && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x4E
                && (header[3] & 0xFF) == 0x47;
    }
}
