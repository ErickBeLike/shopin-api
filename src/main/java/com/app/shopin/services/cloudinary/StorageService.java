package com.app.shopin.services.cloudinary;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface StorageService {

    Map<String, String> uploadImage(MultipartFile file, String subfolder, ImageType imageType);
    Map<String, String> uploadVideo(MultipartFile file, String subfolder);

    Map<String, String> uploadFromUrl(String url, String subfolder, ImageType imageType);

    void deleteFile(String publicId, String resourceType);

    boolean isImageFile(MultipartFile file);
    boolean isVideoFile(MultipartFile file);
}