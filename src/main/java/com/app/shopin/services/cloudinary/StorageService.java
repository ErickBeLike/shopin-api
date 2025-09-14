package com.app.shopin.services.cloudinary;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface StorageService {

    Map<String, String> saveFile(MultipartFile file, String filename, String subfolder);

    Map<String, String> uploadFromUrl(String url, String subfolder);
    void deleteFile(String publicId, String subfolder);

    boolean isImageFile(MultipartFile file);
}