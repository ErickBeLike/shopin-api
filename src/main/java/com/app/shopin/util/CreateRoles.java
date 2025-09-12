package com.app.shopin.util;

import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.security.service.RolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * MUY IMPORTANTE: ESTA CLASE SÓLO SE EJECUTARÁ UNA VEZ PARA CREAR LOS ROLES.
 * UNA VEZ CREADOS SE DEBERÁ ELIMINAR O BIEN COMENTAR EL CÓDIGO
 */

@Component
public class CreateRoles implements CommandLineRunner {

    @Autowired
    RolService rolService;

    @Override
    public void run(String... args) throws Exception {
        /**
         * Sección a descomentar para crear roles
         */

/**
        Rol rolSuperAdmin = new Rol(RolName.ROLE_SUPERADMIN);
        Rol rolAdmin = new Rol(RolName.ROLE_ADMIN);
        Rol rolEmployee = new Rol(RolName.ROLE_EMPLOYEE);
        Rol rolUser = new Rol(RolName.ROLE_USER);

        rolService.save(rolSuperAdmin);
        rolService.save(rolAdmin);
        rolService.save(rolEmployee);
        rolService.save(rolUser);
// **/

    }
}
