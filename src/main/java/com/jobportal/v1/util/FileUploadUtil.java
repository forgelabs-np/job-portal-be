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

    // Agency Document Upload (for agency profile)
    public String uploadAgencyDocument(Long agencyId, String documentType, MultipartFile file) throws IOException {
        validateFile(file);

        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, baseDir, "agency_profiles", String.valueOf(agencyId));

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        String fileName = agencyId + "_" + documentType + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Agency document uploaded to: {}", filePath);

        return Paths.get(baseDir, "agency_profiles", String.valueOf(agencyId), fileName).toString();
    }

    // Agency-Created Candidate Document Upload
    // Add this method to your existing FileUploadUtil class
    public String uploadAgencyCandidateDocument(Long candidateId, String documentType, MultipartFile file) throws IOException {
        validateFile(file);

        String projectRoot = System.getProperty("user.dir");

        // Folder structure: uploads/agency_candidates/{candidateId}/documents/
        Path uploadPath = Paths.get(projectRoot, baseDir, "agency_candidates", String.valueOf(candidateId), "documents");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        // Generate filename: {candidateId}_{documentType}_{timestamp}{extension}
        String fileName = candidateId + "_" + documentType + "_" + System.currentTimeMillis() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Agency candidate document uploaded to: {}", filePath);

        return Paths.get(baseDir, "agency_candidates", String.valueOf(candidateId), "documents", fileName).toString();
    }

    // Self-Registered Candidate Document Upload
    public String uploadSelfCandidateDocument(Long candidateId, String documentType, MultipartFile file) throws IOException {
        validateFile(file);

        String projectRoot = System.getProperty("user.dir");
        Path uploadPath = Paths.get(projectRoot, baseDir, "self_candidates", String.valueOf(candidateId), "documents");

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
        }

        // Generate filename: {candidateId}_{documentType}_{timestamp}{extension}
        String fileName = candidateId + "_" + documentType + "_" + System.currentTimeMillis() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Self candidate document uploaded to: {}", filePath);

        return Paths.get(baseDir, "self_candidates", String.valueOf(candidateId), "documents", fileName).toString();
    }

    // Generic delete file method
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
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + (maxFileSize / 1048576) + "MB");
        }

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