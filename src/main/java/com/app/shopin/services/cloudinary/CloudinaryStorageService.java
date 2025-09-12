package com.app.shopin.services.cloudinary;

import com.app.shopin.modules.exception.CustomException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @Override
    public Map<String, String> saveFile(MultipartFile file, String filename, String subfolder) {
        try {
            // Genera solo el ID único, sin la subcarpeta
            String uniqueId = UUID.randomUUID().toString();

            // Sube el archivo, usando el parámetro 'folder' para la carpeta
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", uniqueId, // <--- Solo el ID único
                    "folder", subfolder, // <--- Se usa el parámetro folder
                    "resource_type", "auto"));

            // Crea un mapa para devolver tanto la URL como el publicId
            Map<String, String> result = new HashMap<>();
            // La URL completa ya tiene la ruta correcta: /folder/public_id.extension
            result.put("url", (String) uploadResult.get("secure_url"));
            // El publicId que guardas debe incluir la carpeta para el borrado
            result.put("publicId", (String) uploadResult.get("public_id")); // <-- ¡Esto es crucial!

            return result;
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir el archivo a Cloudinary");
        }
    }

    @Override
    public void deleteFile(String publicId, String subfolder) {
        try {
            if (publicId == null || publicId.isBlank()) {
                log.warn("Intento de eliminar archivo nulo o vacío. Operación omitida.");
                return;
            }

            log.info("Intentando eliminar archivo con publicId: {}", publicId);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            String destroyResult = (String) result.get("result");
            if ("ok".equals(destroyResult) || "not found".equals(destroyResult)) {
                log.info("Archivo eliminado exitosamente. Resultado: {}", destroyResult);
            } else {
                log.error("Error al eliminar archivo en Cloudinary. PublicId: {}, Resultado: {}", publicId, destroyResult);
                throw new CustomException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al eliminar la imagen en Cloudinary. Resultado: " + destroyResult
                );
            }

        } catch (IOException e) {
            log.error("Error de E/S al intentar eliminar la imagen: {}", e.getMessage());
            throw new CustomException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error de E/S al intentar eliminar la imagen de Cloudinary: " + e.getMessage()
            );
        }
    }

    @Override
    public boolean isImageFile(MultipartFile file) {
        return file.getContentType() != null && file.getContentType().startsWith("image/");
    }
}