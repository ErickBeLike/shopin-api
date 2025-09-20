package com.app.shopin.modules.user.service;

import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.cloudinary.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserCleanupService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void permanentlyDeleteUsers() {
        System.out.println("Ejecutando tarea de limpieza de usuarios...");
        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);

        List<User> usersToDelete = userRepository.findUsersForPermanentDeletion(fifteenDaysAgo);

        if (usersToDelete.isEmpty()) {
            System.out.println("No hay usuarios para eliminar permanentemente.");
            return;
        }

        System.out.println("Se eliminarán " + usersToDelete.size() + " usuarios.");

        for (User user : usersToDelete) {
            if (user.getProfilePicturePublicId() != null) {
                storageService.deleteFile(user.getProfilePicturePublicId(), null);
            }

            userRepository.hardDeleteById(user.getUserId());
        }
        System.out.println("Tarea de limpieza de usuarios completada.");
    }
}
