package services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;

import data.ProfilePictureRepository;
import dto.ProfilePictureDTO;
import models.ProfilePicture;
import models.User;

@Service
@Transactional
public class ProfilePictureService {
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private ProfilePictureRepository profilePictureRepository;
    
    @Autowired
    private UserManagementService userManagementService;

    public ProfilePictureDTO uploadProfilePicture(String userEmail, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || 
                                   contentType.equals("image/jpg") || 
                                   contentType.equals("image/png"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Invalid file type. Only PNG, JPG, and JPEG are allowed.");
        }

        try {
            User user = userManagementService.getAuthenticatedUser(userEmail);
            
            deleteExistingProfilePicture(user);

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String s3Key = user.getId() + "/" + fileName;

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            
            amazonS3.putObject(bucketName, s3Key, file.getInputStream(), metadata);

            ProfilePicture profilePic = new ProfilePicture();
            profilePic.setUser(user);
            profilePic.setFileName(fileName);
            profilePic.setFileType(file.getContentType());
            profilePic.setUrl(String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, amazonS3.getRegionName(), s3Key));
            profilePic.setUploadDate(LocalDateTime.now());

            ProfilePicture savedPic = profilePictureRepository.save(profilePic);
            return convertToDTO(savedPic);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Could not store file");
        }
    }

    public ProfilePictureDTO getProfilePicture(String userEmail) {
        User user = userManagementService.getAuthenticatedUser(userEmail);
        ProfilePicture profilePic = profilePictureRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Profile picture not found"));
        
        String s3Key = user.getId() + "/" + profilePic.getFileName();
        if (!amazonS3.doesObjectExist(bucketName, s3Key)) {
            profilePictureRepository.delete(profilePic);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Profile picture file not found");
        }
        
        return convertToDTO(profilePic);
    }

    public void deleteProfilePicture(String userEmail) {
        User user = userManagementService.getAuthenticatedUser(userEmail);
        ProfilePicture profilePic = profilePictureRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Profile picture not found"));

        try {
            String s3Key = user.getId() + "/" + profilePic.getFileName();
            amazonS3.deleteObject(bucketName, s3Key);
            profilePictureRepository.delete(profilePic);
        } catch (AmazonS3Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Could not delete file from S3");
        }
    }

    private void deleteExistingProfilePicture(User user) {
        profilePictureRepository.findByUserId(user.getId()).ifPresent(pic -> {
            try {
                String s3Key = user.getId() + "/" + pic.getFileName();
                amazonS3.deleteObject(bucketName, s3Key);
                profilePictureRepository.delete(pic);
            } catch (AmazonS3Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error deleting existing file");
            }
        });
    }

    private ProfilePictureDTO convertToDTO(ProfilePicture pic) {
        ProfilePictureDTO dto = new ProfilePictureDTO();
        dto.setId(pic.getId());
        dto.setFileName(pic.getFileName());
        dto.setUrl(pic.getUrl());
        dto.setUploadDate(pic.getUploadDate());
        dto.setUserId(pic.getUser().getId().toString());
        return dto;
    }
}
