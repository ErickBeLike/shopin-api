package com.app.shopin.util;

import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.security.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner { // <-- Nombre recomendado

    @Autowired
    private RolRepository rolRepository;

    @Override
    public void run(String... args) throws Exception {
        // Verifica si están los roles en la BD
        if (rolRepository.count() > 0) {
            return;
        }

        // Itera sobre todos los valores del Enum y crea un Rol por cada uno.
        System.out.println("Creando roles iniciales...");
        for (RolName rolName : RolName.values()) {
            rolRepository.save(new Rol(rolName));
        }
        System.out.println("Roles creados exitosamente.");
    }
}