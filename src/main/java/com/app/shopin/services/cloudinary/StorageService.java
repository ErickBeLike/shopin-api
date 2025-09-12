package com.app.shopin.services.cloudinary;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface StorageService {

    Map<String, String> saveFile(MultipartFile file, String filename, String subfolder);

    void deleteFile(String publicId, String subfolder);

    boolean isImageFile(MultipartFile file);
}