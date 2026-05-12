package com.jobportal.v1.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class FileUploadUtil {

    @Value("${file.upload.base-dir:uploads}")
    private String baseDir;

    @Value("${file.upload.max-file-size:5242880}")
    private long maxFileSize;

    @Value("${file.upload.allowed-extensions:png,jpg,jpeg,pdf,doc,docx}")
    private String allowedExtensions;

    public String uploadAgencyDocument(Long agencyId, String documentType, MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);

        String projectRoot = System.getProperty("user.dir");

        // Create directory structure using baseDir from config
        Path uploadPath = Paths.get(projectRoot, baseDir, "agency_profiles", String.valueOf(agencyId));

        // Create directories if they don't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        // Get file extension
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        // Generate filename: {agencyId}_{DOCUMENT_TYPE}{extension}
        String fileName = agencyId + "_" + documentType + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);

        // Copy file (overwrite if exists)
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded to: {}", filePath);

        // Return relative path for database storage
        return Paths.get(baseDir, "agency_profiles", String.valueOf(agencyId), fileName).toString();
    }

    public boolean deleteFile(String filePath) {
        try {
            String projectRoot = System.getProperty("user.dir");
            Path fullPath = Paths.get(projectRoot, filePath);
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    private void validateFile(MultipartFile file) {
        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + (maxFileSize / 1048576) + "MB");
        }

        // Validate file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new RuntimeException("Invalid file format. File must have an extension.");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase().replace(".", "");
        List<String> allowedList = Arrays.asList(allowedExtensions.split(","));

        if (!allowedList.contains(extension)) {
            throw new RuntimeException("File type not allowed. Allowed types: " + allowedExtensions);
        }
    }
}