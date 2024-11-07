package services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.timgroup.statsd.StatsDClient;

import data.ProfilePictureRepository;
import dto.ProfilePictureDTO;
import models.ProfilePicture;
import models.User;

@Service
@Transactional
@ConditionalOnProperty(name = "aws.s3.bucket")
public class ProfilePictureService {
   private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);
   
   @Value("${aws.s3.bucket}")
   private String bucketName;
   
   @Autowired
   private AmazonS3 amazonS3;

   @Autowired
   private ProfilePictureRepository profilePictureRepository;
   
   @Autowired
   private UserManagementService userManagementService;
   
   @Autowired
   private S3LoggingService s3LoggingService;

   @Autowired
   private StatsDClient statsDClient;

   public ProfilePictureDTO uploadProfilePicture(String userEmail, MultipartFile file) {
       System.out.println("DEBUG: Starting uploadProfilePicture for user: " + userEmail);
       System.out.println("DEBUG: File details - Name: " + file.getOriginalFilename() + 
                         ", Size: " + file.getSize() + ", Type: " + file.getContentType());

       long startTime = System.currentTimeMillis();
       statsDClient.incrementCounter("api.profile.picture.upload.count");
       logger.info("Starting profile picture upload for user: {}", userEmail);
       
       try {
           System.out.println("DEBUG: Starting file validation");
           validateUploadRequest(file);
           System.out.println("DEBUG: File validation passed successfully");

           System.out.println("DEBUG: Getting authenticated user");
           User user = userManagementService.getAuthenticatedUser(userEmail);
           System.out.println("DEBUG: Found user with ID: " + user.getId());

           System.out.println("DEBUG: Checking for existing profile picture");
           deleteExistingProfilePicture(user);

           String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
           String s3Key = user.getId() + "/" + fileName;
           System.out.println("DEBUG: Generated S3 key: " + s3Key);

           long s3StartTime = System.currentTimeMillis();
           System.out.println("DEBUG: Starting S3 upload");
           uploadToS3(file, s3Key);
           long s3Duration = System.currentTimeMillis() - s3StartTime;
           System.out.println("DEBUG: S3 upload completed in " + s3Duration + "ms");
           
           s3LoggingService.logS3Operation("UPLOAD", bucketName, s3Key, s3Duration);
           statsDClient.recordExecutionTime("s3.upload.time", s3Duration);

           System.out.println("DEBUG: Creating profile picture record");
           long dbStartTime = System.currentTimeMillis();
           ProfilePicture profilePic = createProfilePicture(user, fileName, file, s3Key);
           ProfilePicture savedPic = profilePictureRepository.save(profilePic);
           long dbDuration = System.currentTimeMillis() - dbStartTime;
           System.out.println("DEBUG: Database save completed in " + dbDuration + "ms");
           System.out.println("DEBUG: Saved profile picture with ID: " + savedPic.getId());
           
           statsDClient.recordExecutionTime("db.save.time", dbDuration);

           logger.info("Profile picture uploaded successfully for user: {}", userEmail);
           statsDClient.recordExecutionTime("api.profile.picture.upload.time", System.currentTimeMillis() - startTime);
           
           System.out.println("DEBUG: Upload process completed successfully");
           return convertToDTO(savedPic);

       } catch (IOException e) {
           System.out.println("ERROR: IO Exception during upload: " + e.getMessage());
           statsDClient.incrementCounter("api.profile.picture.upload.error");
           logger.error("Failed to upload profile picture for user {}: {}", userEmail, e.getMessage());
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file", e);
       } catch (AmazonS3Exception e) {
           System.out.println("ERROR: S3 Exception during upload: " + e.getMessage());
           statsDClient.incrementCounter("api.profile.picture.upload.error");
           statsDClient.incrementCounter("s3.upload.error");
           logger.error("S3 error while uploading profile picture for user {}: {}", userEmail, e.getMessage());
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file in S3", e);
       } catch (Exception e) {
           System.out.println("ERROR: Unexpected exception during upload: " + e.getMessage());
           statsDClient.incrementCounter("api.profile.picture.upload.error");
           logger.error("Unexpected error while uploading profile picture for user {}: {}", userEmail, e.getMessage());
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process upload request", e);
       }
   }

   private void uploadToS3(MultipartFile file, String s3Key) throws IOException {
       System.out.println("DEBUG: Starting S3 upload - Key: " + s3Key + ", Bucket: " + bucketName);
       
       ObjectMetadata metadata = new ObjectMetadata();
       metadata.setContentType(file.getContentType());
       metadata.setContentLength(file.getSize());
       
       try {
           System.out.println("DEBUG: Initiating S3 putObject operation");
           amazonS3.putObject(bucketName, s3Key, file.getInputStream(), metadata);
           System.out.println("DEBUG: S3 upload completed successfully");
       } catch (AmazonS3Exception e) {
           System.out.println("ERROR: S3 upload failed: " + e.getMessage());
           statsDClient.incrementCounter("s3.upload.error");
           logger.error("Failed to upload file to S3: {}", e.getMessage());
           throw e;
       }
   }

   private void deleteExistingProfilePicture(User user) {
       System.out.println("DEBUG: Checking for existing profile picture - User ID: " + user.getId());
       logger.info("Checking for existing profile picture for user: {}", user.getEmail());
       
       profilePictureRepository.findByUserId(user.getId()).ifPresent(pic -> {
           try {
               String s3Key = user.getId() + "/" + pic.getFileName();
               System.out.println("DEBUG: Found existing picture, preparing to delete - S3 Key: " + s3Key);
               
               long s3StartTime = System.currentTimeMillis();
               System.out.println("DEBUG: Deleting from S3");
               amazonS3.deleteObject(bucketName, s3Key);
               long s3Duration = System.currentTimeMillis() - s3StartTime;
               System.out.println("DEBUG: S3 deletion completed in " + s3Duration + "ms");
               
               s3LoggingService.logS3Operation("DELETE", bucketName, s3Key, s3Duration);
               statsDClient.recordExecutionTime("s3.delete.time", s3Duration);
               
               long dbStartTime = System.currentTimeMillis();
               System.out.println("DEBUG: Deleting from database");
               profilePictureRepository.delete(pic);
               System.out.println("DEBUG: Database deletion completed in " + (System.currentTimeMillis() - dbStartTime) + "ms");
               
               statsDClient.recordExecutionTime("db.delete.time", System.currentTimeMillis() - dbStartTime);
               
               logger.info("Deleted existing profile picture for user: {}", user.getEmail());
           } catch (AmazonS3Exception e) {
               System.out.println("ERROR: Failed to delete from S3: " + e.getMessage());
               statsDClient.incrementCounter("s3.delete.error");
               logger.error("Failed to delete existing S3 object for user {}: {}", 
                   user.getEmail(), e.getMessage());
               throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                   "Failed to delete existing file from S3", e);
           }
       });
   }

   public ProfilePictureDTO getProfilePicture(String userEmail) {
       System.out.println("DEBUG: Starting getProfilePicture for user: " + userEmail);
       
       long startTime = System.currentTimeMillis();
       statsDClient.incrementCounter("api.profile.picture.get.count");
       logger.info("Retrieving profile picture for user: {}", userEmail);
       
       try {
           System.out.println("DEBUG: Getting authenticated user");
           User user = userManagementService.getAuthenticatedUser(userEmail);
           System.out.println("DEBUG: Found user with ID: " + user.getId());
           
           System.out.println("DEBUG: Querying database for profile picture");
           long dbStartTime = System.currentTimeMillis();
           ProfilePicture profilePic = profilePictureRepository.findByUserId(user.getId())
               .orElseThrow(() -> {
                   System.out.println("ERROR: Profile picture not found in database");
                   logger.error("Profile picture not found for user: {}", userEmail);
                   statsDClient.incrementCounter("api.profile.picture.not_found");
                   return new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile picture not found");
               });
           System.out.println("DEBUG: Database query completed in " + (System.currentTimeMillis() - dbStartTime) + "ms");
           statsDClient.recordExecutionTime("db.query.time", System.currentTimeMillis() - dbStartTime);
           
           String s3Key = user.getId() + "/" + profilePic.getFileName();
           System.out.println("DEBUG: Checking S3 object exists - Key: " + s3Key);
           
           long s3StartTime = System.currentTimeMillis();
           boolean exists = amazonS3.doesObjectExist(bucketName, s3Key);
           long s3Duration = System.currentTimeMillis() - s3StartTime;
           System.out.println("DEBUG: S3 check completed in " + s3Duration + "ms - Exists: " + exists);
           
           s3LoggingService.logS3Operation("CHECK_EXISTS", bucketName, s3Key, s3Duration);
           statsDClient.recordExecutionTime("s3.check.time", s3Duration);
           
           if (!exists) {
               System.out.println("ERROR: S3 object not found");
               statsDClient.incrementCounter("s3.check.missing");
               logger.error("S3 object not found for user {}: {}", userEmail, s3Key);
               profilePictureRepository.delete(profilePic);
               throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile picture file not found");
           }
           
           logger.info("Successfully retrieved profile picture for user: {}", userEmail);
           statsDClient.recordExecutionTime("api.profile.picture.get.time", System.currentTimeMillis() - startTime);
           
           System.out.println("DEBUG: Successfully retrieved profile picture");
           return convertToDTO(profilePic);

       } catch (ResponseStatusException e) {
           System.out.println("ERROR: Response status exception: " + e.getMessage());
           throw e;
       } catch (Exception e) {
           System.out.println("ERROR: Unexpected exception while getting profile picture: " + e.getMessage());
           statsDClient.incrementCounter("api.profile.picture.get.error");
           logger.error("Error retrieving profile picture for user {}: {}", userEmail, e.getMessage());
           throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve profile picture", e);
       }
   }

   public void deleteProfilePicture(String userEmail) {
       System.out.println("DEBUG: Starting deleteProfilePicture for user: " + userEmail);
       
       long startTime = System.currentTimeMillis();
       statsDClient.incrementCounter("api.profile.picture.delete.count");
       logger.info("Deleting profile picture for user: {}", userEmail);
       
       try {
           System.out.println("DEBUG: Getting authenticated user");
           User user = userManagementService.getAuthenticatedUser(userEmail);
           System.out.println("DEBUG: Found user with ID: " + user.getId());
           
           System.out.println("DEBUG: Querying database for profile picture");
           long dbStartTime = System.currentTimeMillis();
           ProfilePicture profilePic = profilePictureRepository.findByUserId(user.getId())
               .orElseThrow(() -> {
                   System.out.println("ERROR: No profile picture found to delete");
                   logger.error("No profile picture found to delete for user: {}", userEmail);
                   statsDClient.incrementCounter("api.profile.picture.not_found");
                   return new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile picture not found");
               });
           System.out.println("DEBUG: Database query completed in " + (System.currentTimeMillis() - dbStartTime) + "ms");
           statsDClient.recordExecutionTime("db.query.time", System.currentTimeMillis() - dbStartTime);

           String s3Key = user.getId() + "/" + profilePic.getFileName();
           System.out.println("DEBUG: Deleting from S3 - Key: " + s3Key);
           
           try {
               long s3StartTime = System.currentTimeMillis();
               amazonS3.deleteObject(bucketName, s3Key);
               long s3Duration = System.currentTimeMillis() - s3StartTime;
               System.out.println("DEBUG: S3 deletion completed in " + s3Duration + "ms");
               
               s3LoggingService.logS3Operation("DELETE", bucketName, s3Key, s3Duration);
               statsDClient.recordExecutionTime("s3.delete.time", s3Duration);
           } catch (AmazonS3Exception e) {
               System.out.println("ERROR: Failed to delete from S3: " + e.getMessage());
               statsDClient.incrementCounter("s3.delete.error");
               logger.error("Failed to delete S3 object for user {}: {}", userEmail, e.getMessage());
               throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file from S3", e);
           }
           
           System.out.println("DEBUG: Deleting from database");
           long dbDeleteStart = System.currentTimeMillis();
           profilePictureRepository.delete(profilePic);
           System.out.println("DEBUG: Database deletion completed in " + (System.currentTimeMillis() - dbDeleteStart) + "ms");
           statsDClient.recordExecutionTime("db.delete.time", System.currentTimeMillis() - dbDeleteStart);
           
           logger.info("Successfully deleted profile picture for user: {}", userEmail);
           statsDClient.recordExecutionTime("api.profile.picture.delete.time", System.currentTimeMillis() - startTime);
           System.out.println("DEBUG: Delete operation completed successfully");

       } catch (ResponseStatusException e) {
           System.out.println("ERROR: Response status exception: " + e.getMessage());
           throw e;
        } catch (Exception e) {
            System.out.println("ERROR: Unexpected exception while deleting profile picture: " + e.getMessage());
            statsDClient.incrementCounter("api.profile.picture.delete.error");
            logger.error("Error deleting profile picture for user {}: {}", userEmail, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete profile picture", e);
        }
    }
    
    private void validateUploadRequest(MultipartFile file) {
        System.out.println("DEBUG: Starting file validation");
        System.out.println("DEBUG: Checking file - IsEmpty: " + (file == null || file.isEmpty()) + 
            ", ContentType: " + file.getContentType());
 
        if (file == null || file.isEmpty()) {
            System.out.println("ERROR: File validation failed - Empty file");
            statsDClient.incrementCounter("api.profile.picture.validation.error");
            logger.error("Upload failed: Empty file");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }
 
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || 
                                   contentType.equals("image/jpg") || 
                                   contentType.equals("image/png"))) {
            System.out.println("ERROR: File validation failed - Invalid content type: " + contentType);
            statsDClient.incrementCounter("api.profile.picture.validation.error");
            logger.error("Upload failed: Invalid file type {}", contentType);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid file type. Only PNG, JPG, and JPEG are allowed.");
        }
        System.out.println("DEBUG: File validation passed successfully");
    }
 
    private ProfilePicture createProfilePicture(User user, String fileName, MultipartFile file, String s3Key) {
        System.out.println("DEBUG: Creating ProfilePicture entity");
        ProfilePicture profilePic = new ProfilePicture();
        profilePic.setUser(user);
        profilePic.setFileName(fileName);
        profilePic.setFileType(file.getContentType());
        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", 
            bucketName, amazonS3.getRegionName(), s3Key);
        profilePic.setUrl(url);
        profilePic.setUploadDate(LocalDateTime.now());
        System.out.println("DEBUG: Created ProfilePicture entity with URL: " + url);
        return profilePic;
    }
 
    private ProfilePictureDTO convertToDTO(ProfilePicture pic) {
        System.out.println("DEBUG: Converting ProfilePicture to DTO");
        ProfilePictureDTO dto = new ProfilePictureDTO();
        dto.setId(pic.getId());
        dto.setFileName(pic.getFileName());
        dto.setUrl(pic.getUrl());
        dto.setUploadDate(pic.getUploadDate());
        dto.setUserId(pic.getUser().getId().toString());
        System.out.println("DEBUG: Created DTO with ID: " + dto.getId() + " and URL: " + dto.getUrl());
        return dto;
    }
 }