package com.jobportal.v1.util;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@Slf4j
public class FileUploadUtil {

    private final ServletContext servletContext;

    public FileUploadUtil(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String uploadAgencyDocument(Long agencyId, String documentType, MultipartFile file) throws IOException {
        // Get the real path to the web application root
        String realPath = servletContext.getRealPath("/");
        if (realPath == null) {
            realPath = System.getProperty("user.dir") + "/uploads";
        }

        Path uploadPath = Paths.get(realPath, "uploads", "agency_profiles", String.valueOf(agencyId), documentType);

        // Create directories if they don't exist
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        log.info("File uploaded to: {}", filePath);

        // Return relative path for database storage
        return Paths.get("uploads", "agency_profiles", String.valueOf(agencyId), documentType, fileName).toString();
    }

    public boolean deleteFile(String filePath) {
        try {
            String realPath = servletContext.getRealPath("/");
            if (realPath == null) {
                realPath = System.getProperty("user.dir") + "/uploads";
            }
            Path fullPath = Paths.get(realPath, filePath);
            return Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }
}