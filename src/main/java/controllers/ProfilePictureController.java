package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.timgroup.statsd.StatsDClient;

import dto.ProfilePictureDTO;
import services.ProfilePictureService;

@RestController
@RequestMapping("/v1/user/self")
public class ProfilePictureController {
    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureController.class);
    
    @Autowired
    private ProfilePictureService profilePictureService;
    
    @Autowired
    private StatsDClient statsDClient;
    
    @PostMapping(value = "/pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePictureDTO> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        logger.info("Starting profile picture upload for user: {}", authentication.getName());
        logger.debug("File details - size: {}, contentType: {}, name: {}", 
            file.getSize(), file.getContentType(), file.getOriginalFilename());
            
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("api.pic.post.count");
        
        try {
            ProfilePictureDTO profilePic = profilePictureService.uploadProfilePicture(
                authentication.getName(),
                file
            );
            
            long latency = System.currentTimeMillis() - startTime;
            statsDClient.recordExecutionTime("api.pic.post.time", latency);
            logger.info("Successfully uploaded profile picture for user: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(profilePic);
        } catch (Exception e) {
            logger.error("Failed to upload profile picture for user: {} - Error: {}", 
                authentication.getName(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/pic")
    public ResponseEntity<ProfilePictureDTO> getProfilePicture(Authentication authentication) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("api.pic.get.count");

        ProfilePictureDTO profilePic = profilePictureService.getProfilePicture(authentication.getName());

        long latency = System.currentTimeMillis() - startTime;
        statsDClient.recordExecutionTime("api.pic.get.time", latency);
        return ResponseEntity.ok(profilePic);
    }

    @DeleteMapping("/pic")
    public ResponseEntity<Void> deleteProfilePicture(Authentication authentication) {
        long startTime = System.currentTimeMillis();
        statsDClient.incrementCounter("api.pic.delete.count");

        profilePictureService.deleteProfilePicture(authentication.getName());

        long latency = System.currentTimeMillis() - startTime;
        statsDClient.recordExecutionTime("api.pic.delete.time", latency);
        return ResponseEntity.noContent().build();
    }
}