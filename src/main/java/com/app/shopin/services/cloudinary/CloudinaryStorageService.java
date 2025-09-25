package com.app.shopin.services.cloudinary;

import com.app.shopin.modules.exception.CustomException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
public class CloudinaryStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageService.class);

    private final Cloudinary cloudinary;

    @Autowired
    public CloudinaryStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    private Map<String, String> upload(byte[] fileBytes, String subfolder, String resourceType, ImageType imageType) {
        try {
            String uniqueId = UUID.randomUUID().toString();
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("public_id", uniqueId);
            uploadParams.put("folder", subfolder);
            uploadParams.put("resource_type", resourceType);

            if ("image".equals(resourceType) && imageType != null) {
                Transformation transformation = new Transformation();
                switch (imageType) {
                    case PROFILE:
                        // Receta para fotos de perfil: 96x96, enfocada en la cara.
                        transformation.width(96).height(96).gravity("face").crop("thumb");
                        break;
                    case PRODUCT:
                        // Receta para fotos de producto: máximo 1080x1080, sin deformar.
                        transformation.width(1080).height(1080).crop("limit");
                        break;
                }
                uploadParams.put("transformation", transformation);
            }

            Map uploadResult = cloudinary.uploader().upload(fileBytes, uploadParams);

            Map<String, String> result = new HashMap<>();
            result.put("url", (String) uploadResult.get("secure_url"));
            result.put("publicId", (String) uploadResult.get("public_id"));
            return result;
        } catch (IOException e) {
            log.error("Error al subir archivo a Cloudinary", e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir el archivo.");
        }
    }

    @Override
    public Map<String, String> uploadImage(MultipartFile file, String subfolder, ImageType imageType) {
        if (!isImageFile(file)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El archivo proporcionado no es una imagen válida.");
        } try {
            return upload(file.getBytes(), subfolder, "image", imageType);
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo de imagen.");
        }
    }

    @Override
    public Map<String, String> uploadVideo(MultipartFile file, String subfolder) {
        if (!isVideoFile(file)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El archivo proporcionado no es un video válido.");
        } try {
            // CORRECCIÓN: Le pasamos los bytes del archivo y null para el ImageType
            return upload(file.getBytes(), subfolder, "video", null);
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al leer el archivo de video.");
        }
    }

    @Override
    public Map<String, String> uploadFromUrl(String url, String subfolder, ImageType imageType) {
        try {
            byte[] bytes = new URL(url).openStream().readAllBytes();
            return upload(bytes, subfolder, "image", imageType);
        } catch (IOException e) {
            log.error("Error al procesar la imagen desde la URL: {}", url, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar la imagen desde la URL.");
        }
    }

    @Override
    public void deleteFile(String publicId, String resourceType) {
        try {
            if (publicId == null || publicId.isBlank()) return;
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", resourceType));
        } catch (IOException e) {
            log.error("Error al eliminar archivo de Cloudinary: {}", publicId, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al eliminar el archivo.");
        }
    }

    @Override
    public boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public boolean isVideoFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/");
    }
}