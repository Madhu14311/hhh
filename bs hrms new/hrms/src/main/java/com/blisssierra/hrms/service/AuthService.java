package com.blisssierra.hrms.service;
import com.blisssierra.hrms.dto.*;
import com.blisssierra.hrms.entity.Employee;
import com.blisssierra.hrms.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private OtpService otpService;
    @Autowired
    private FileStorageService fileStorageService;
    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1: Signup — save employee (unverified), send OTP
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Handles POST /api/auth/signup
     *
     * 1. Validates email/empId uniqueness
     * 2. Saves employee as unverified (face paths not set yet)
     * 3. Sends OTP to the provided email
     *
     * The employee record is saved NOW so subsequent face-upload can find it by
     * email.
     * verified = false until OTP is confirmed.
     */
    public ApiResponseDto signup(SignupRequestDto req) {
        log.info("Signup request: empId={}, email={}", req.getEmpId(), req.getEmail());
        // Validate required fields
        if (isBlank(req.getName()) || isBlank(req.getEmail()) ||
                isBlank(req.getEmpId()) || isBlank(req.getDesignation()) ||
                isBlank(req.getPassword())) {
            return new ApiResponseDto("error", "All fields are required");
        }
        String email = req.getEmail().trim().toLowerCase();
        String empId = req.getEmpId().trim().toUpperCase();
        // Check for duplicate email
        if (employeeRepository.existsByEmail(email)) {
            // If already registered but NOT verified, allow re-registration
            Optional<Employee> existing = employeeRepository.findByEmail(email);
            if (existing.isPresent() && existing.get().isVerified()) {
                return new ApiResponseDto("error", "An account with this email already exists");
            }
            // Unverified → delete old record and re-register
            existing.ifPresent(e -> {
                fileStorageService.deleteFaceImages(e.getEmpId());
                employeeRepository.delete(e);
                log.info("Deleted unverified duplicate account for email={}", email);
            });
        }
        // Check for duplicate empId
        if (employeeRepository.existsByEmpId(empId)) {
            Optional<Employee> existing = employeeRepository.findByEmpId(empId);
            if (existing.isPresent() && existing.get().isVerified()) {
                return new ApiResponseDto("error", "An account with this Employee ID already exists");
            }
            existing.ifPresent(e -> {
                fileStorageService.deleteFaceImages(e.getEmpId());
                employeeRepository.delete(e);
            });
        }
        // Save employee record (unverified, no face paths yet)
        Employee employee = new Employee();
        employee.setName(req.getName().trim());
        employee.setEmail(email);
        employee.setEmpId(empId);
        employee.setDesignation(req.getDesignation().trim());
        employee.setPassword(req.getPassword()); // Plain text — see NOTE below
        employee.setVerified(false);
        employee.setFaceImagePaths("");
        employeeRepository.save(employee);
        log.info("Employee saved (unverified): empId={}, email={}", empId, email);
        /*
         * NOTE ON PASSWORDS:
         * For production use BCryptPasswordEncoder. For this project we store plain
         * text
         * so the login check is straightforward without adding Spring Security
         * dependency.
         * To add BCrypt: inject PasswordEncoder, call encoder.encode(req.getPassword())
         * here, and encoder.matches(raw, stored) in login().
         */
        // Send OTP
        try {
            otpService.generateAndSendOtp(email, req.getName().trim());
        } catch (Exception e) {
            log.error("OTP send failed for {}: {}", email, e.getMessage());
            // Delete the employee we just saved to keep DB clean
            employeeRepository.delete(employee);
            return new ApiResponseDto("error",
                    "Registration failed: Could not send OTP email. " +
                            "Please check your email address and try again. Error: " + e.getMessage());
        }
        return new ApiResponseDto("success",
                "OTP sent to " + email + ". Please check your inbox and complete face registration first.");
    }
    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2: Upload Face Images — save 3 selfies, store paths in DB
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Handles POST /api/auth/upload-face (multipart)
     *
     * 1. Validates employee exists (must have signed up first)
     * 2. Saves 3 face images to uploads/faces/{empId}/
     * 3. Stores the absolute paths in employee.faceImagePaths (comma-separated)
     *
     * These absolute paths are later sent to Python FastAPI for face comparison
     * during check-in / check-out.
     */
    public FaceUploadResponseDto uploadFace(String email, List<MultipartFile> images) {
        log.info("Face upload request: email={}, imageCount={}", email, images == null ? 0 : images.size());
        if (isBlank(email)) {
            return new FaceUploadResponseDto("error", "Email is required", null);
        }
        String normalizedEmail = email.trim().toLowerCase();
        Optional<Employee> optional = employeeRepository.findByEmail(normalizedEmail);
        if (optional.isEmpty()) {
            return new FaceUploadResponseDto("error",
                    "No registration found for " + normalizedEmail + ". Please sign up first.", null);
        }
        Employee employee = optional.get();
        if (images == null || images.isEmpty()) {
            return new FaceUploadResponseDto("error", "No images received", null);
        }
        // Filter out null/empty entries
        List<MultipartFile> validImages = images.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();
        if (validImages.size() < 3) {
            return new FaceUploadResponseDto("error",
                    "Exactly 3 face images are required. Received: " + validImages.size(), null);
        }
        // Save images to disk and get absolute paths
        List<String> paths;
        try {
            paths = fileStorageService.saveFaceImages(employee.getEmpId(), validImages.subList(0, 3));
        } catch (Exception e) {
            log.error("File save error for empId={}: {}", employee.getEmpId(), e.getMessage());
            return new FaceUploadResponseDto("error", "Failed to save face images: " + e.getMessage(), null);
        }
        if (paths.isEmpty()) {
            return new FaceUploadResponseDto("error", "No images were saved successfully", null);
        }
        // Store comma-separated absolute paths in DB
        String pathsCsv = String.join(",", paths);
        employee.setFaceImagePaths(pathsCsv);
        employeeRepository.save(employee);
        log.info("✅ Face images saved for empId={}: {}", employee.getEmpId(), pathsCsv);
        return new FaceUploadResponseDto("success",
                "Face images uploaded successfully (" + paths.size() + " photos stored)", pathsCsv);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // STEP 3: Verify OTP — mark employee as verified
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Handles POST /api/auth/verify-otp
     *
     * Validates OTP and marks employee as verified.
     * Employee can now log in.
     */
    public ApiResponseDto verifyOtp(OtpVerifyRequestDto req) {
        log.info("OTP verify request: email={}", req.getEmail());
        if (isBlank(req.getEmail()) || isBlank(req.getOtp())) {
            return new ApiResponseDto("error", "Email and OTP are required");
        }
        String email = req.getEmail().trim().toLowerCase();
        boolean valid = otpService.validateOtp(email, req.getOtp().trim());
        if (!valid) {
            return new ApiResponseDto("error", "Invalid or expired OTP. Please request a new one.");
        }
        // Mark employee as verified
        Optional<Employee> optional = employeeRepository.findByEmail(email);
        if (optional.isPresent()) {
            Employee employee = optional.get();
            employee.setVerified(true);
            employeeRepository.save(employee);
            log.info("✅ Employee verified: empId={}, email={}", employee.getEmpId(), email);
        } else {
            log.warn("OTP verified but employee not found: {}", email);
        }
        return new ApiResponseDto("success", "Email verified successfully! You can now sign in.");
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Resend OTP
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Handles POST /api/auth/resend-otp
     */
    public ApiResponseDto resendOtp(ResendOtpRequestDto req) {
        log.info("Resend OTP request: email={}", req.getEmail());
        if (isBlank(req.getEmail())) {
            return new ApiResponseDto("error", "Email is required");
        }
        String email = req.getEmail().trim().toLowerCase();
        Optional<Employee> optional = employeeRepository.findByEmail(email);
        if (optional.isEmpty()) {
            return new ApiResponseDto("error", "No account found for " + email);
        }
        try {
            otpService.generateAndSendOtp(email, optional.get().getName());
        } catch (Exception e) {
            return new ApiResponseDto("error", "Failed to resend OTP: " + e.getMessage());
        }
        return new ApiResponseDto("success", "OTP resent to " + email);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Handles POST /api/auth/login
     *
     * Returns all employee profile data needed by the frontend:
     * name, email, empId, designation, faceImagePaths
     *
     * faceImagePaths is the comma-separated absolute paths stored during face
     * upload.
     * The frontend stores this in UserContext.
     * When check-in is triggered, Attendance.js sends empId to Spring Boot,
     * which looks up faceImagePaths from DB and passes them to Python FastAPI.
     */
    public LoginResponseDto login(LoginRequestDto req) {
        log.info("Login request: empId={}", req.getEmpId());
        if (isBlank(req.getEmpId()) || isBlank(req.getPassword())) {
            return errorLogin("Employee ID and password are required");
        }
        String empId = req.getEmpId().trim().toUpperCase();
        Optional<Employee> optional = employeeRepository.findByEmpId(empId);
        if (optional.isEmpty()) {
            log.warn("Login failed: empId={} not found", empId);
            return errorLogin("Invalid Employee ID or password");
        }
        Employee employee = optional.get();
        // Check password (plain text comparison)
        if (!employee.getPassword().equals(req.getPassword().trim())) {
            log.warn("Login failed: wrong password for empId={}", empId);
            return errorLogin("Invalid Employee ID or password");
        }
        // Check verified
        if (!employee.isVerified()) {
            log.warn("Login failed: empId={} not verified", empId);
            return errorLogin("Your account is not verified. Please complete the OTP verification during signup.");
        }
        log.info("✅ Login success: empId={}, name={}", empId, employee.getName());
        LoginResponseDto res = new LoginResponseDto();
        res.setStatus("success");
        res.setMessage("Login successful");
        res.setUserId(employee.getId()); // 👈 MAIN FIX
        res.setName(employee.getName());
        res.setEmail(employee.getEmail());
        res.setEmpId(employee.getEmpId());
        res.setDesignation(employee.getDesignation());
        res.setFaceImagePaths(employee.getFaceImagePaths());
        return res;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private LoginResponseDto errorLogin(String message) {
        LoginResponseDto res = new LoginResponseDto();
        res.setStatus("error");
        res.setMessage(message);
        return res;
    }
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
