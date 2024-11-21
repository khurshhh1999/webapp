package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.timgroup.statsd.StatsDClient;

import dto.ProfilePictureDTO;
import services.ProfilePictureService;

@RestController
@RequestMapping("/v1/user/self")
public class ProfilePictureController {

    private static final Logger log = LoggerFactory.getLogger(ProfilePictureController.class);

    @Autowired
    private ProfilePictureService profilePictureService;

    @Autowired
    private StatsDClient statsDClient;

    @PostMapping(value = "/pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePictureDTO> uploadProfilePicture(
            Authentication authentication,
            @RequestParam(name = "file") MultipartFile file) {

        log.info("Received file upload request");
        String username = authentication.getName();
        long startTime = System.currentTimeMillis();

        MDC.put("user", username);
        MDC.put("operation", "upload_profile_pic");

        // Set request attributes for access logging
        

        try {
            log.info("Starting profile picture upload - Size: {}B, Type: {}, Name: {}",
                    file.getSize(), file.getContentType(), file.getOriginalFilename());

            validateFile(file);

            ProfilePictureDTO result = profilePictureService.uploadProfilePicture(username, file);

            long duration = System.currentTimeMillis() - startTime;
            statsDClient.recordExecutionTime("api.pic.upload.time", duration);
            statsDClient.incrementCounter("api.pic.upload.success");

            log.info("Profile picture upload successful - Duration: {}ms", duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            statsDClient.incrementCounter("api.pic.upload.error");

            // Set error attribute for access logging
            log.error("Profile picture upload failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/pic")
    public ResponseEntity<ProfilePictureDTO> getProfilePicture(
            Authentication authentication) {

        String username = authentication.getName();

        MDC.put("user", username);
        MDC.put("operation", "get_profile_pic");

        // Set request attributes for access logging
        

        try {
            log.info("Fetching profile picture");
            ProfilePictureDTO result = profilePictureService.getProfilePicture(username);

            statsDClient.incrementCounter("api.pic.get.success");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            statsDClient.incrementCounter("api.pic.get.error");

            // Set error attribute for access logging
            log.error("Failed to fetch profile picture: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/pic")
    public ResponseEntity<Void> deleteProfilePicture(
            Authentication authentication) {

        String username = authentication.getName();

        MDC.put("user", username);
        MDC.put("operation", "delete_profile_pic");
        try {
            log.info("Deleting profile picture");
            profilePictureService.deleteProfilePicture(username);

            statsDClient.incrementCounter("api.pic.delete.success");
            log.info("Profile picture deleted successfully");
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            statsDClient.incrementCounter("api.pic.delete.error");

            // Set error attribute for access logging

            log.error("Failed to delete profile picture: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            log.info("Empty file uploaded");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.info("Invalid file type: {}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        if (file.getSize() > 1024 * 1024) { // 1MB limit
            log.info("File too large: {}B", file.getSize());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must be less than 1MB");
        }
    }
}
