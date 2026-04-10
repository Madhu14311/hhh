package com.blisssierra.hrms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles saving face images to the local file system.
 *
 * Directory structure created:
 * {app.upload.dir}/
 * EMP001/
 * face_1.jpg
 * face_2.jpg
 * face_3.jpg
 * EMP002/
 * face_1.jpg
 * ...
 *
 * The ABSOLUTE paths are stored in the DB so Python FastAPI can read them
 * directly.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Save multiple face images for an employee.
     * Returns list of ABSOLUTE file-system paths (used by Python for face
     * comparison).
     *
     * @param empId  employee ID (used as sub-folder name)
     * @param images list of uploaded image files (exactly 3)
     * @return list of absolute paths to saved images
     */
    public List<String> saveFaceImages(String empId, List<MultipartFile> images) throws IOException {
        // Resolve the upload directory to an absolute path
        Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path empDir = basePath.resolve(empId);

        // Create directory if it doesn't exist
        Files.createDirectories(empDir);
        log.info("Saving face images for empId={} to: {}", empId, empDir);

        List<String> savedPaths = new ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile file = images.get(i);

            if (file == null || file.isEmpty()) {
                log.warn("Image {} is null or empty — skipping", i + 1);
                continue;
            }

            // Always save as face_1.jpg, face_2.jpg, face_3.jpg
            String filename = "face_" + (i + 1) + ".jpg";
            Path targetPath = empDir.resolve(filename);

            // Copy file bytes to disk (overwrite if re-registering)
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String absolutePath = targetPath.toAbsolutePath().toString();
            savedPaths.add(absolutePath);
            log.info("  Saved: {}", absolutePath);
        }

        if (savedPaths.isEmpty()) {
            throw new IOException("No valid images were saved for empId=" + empId);
        }

        log.info("Total face images saved for {}: {}", empId, savedPaths.size());
        return savedPaths;
    }

    /**
     * Delete the face images folder for an employee (used when re-registering).
     */
    public void deleteFaceImages(String empId) {
        try {
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path empDir = basePath.resolve(empId);
            if (Files.exists(empDir)) {
                Files.walk(empDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Deleted face images folder for empId={}", empId);
            }
        } catch (IOException e) {
            log.warn("Could not delete face images for empId={}: {}", empId, e.getMessage());
        }
    }

    /**
     * Get the absolute path for the upload directory (for debugging).
     */
    public String getUploadDirAbsolutePath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize().toString();
    }
}
