package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.mailtrap.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    EmailService emailService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String userInput) throws UsernameNotFoundException {

        // 1. Intenta encontrar al usuario de forma normal (activo)
        Optional<User> activeUser = userRepository.findByUserNameOrEmail(userInput, userInput);
        if (activeUser.isPresent()) {
            return PrincipalUser.build(activeUser.get());
        }

        // 2. Si no se encuentra, busca un usuario inactivo
        Optional<User> inactiveUser = userRepository.findInactiveByUsernameOrEmail(userInput);

        // 3. Si se encuentra un usuario inactivo, reactívalo
        if (inactiveUser.isPresent()) {
            User userToReactivate = inactiveUser.get();
            userRepository.reactivateUserById(userToReactivate.getUserId());

            // 4. Envía el correo de reactivación
            emailService.sendReactivationEmail(userToReactivate.getEmail(), userToReactivate.getUserName());

            // 5. Retorna el usuario reactivado para el login
            // Debes obtener la instancia reactivada para asegurarte de que el estado es correcto
            User reactivatedUser = userRepository.findByUserNameOrEmail(userInput, userInput)
                    .orElseThrow(() -> new UsernameNotFoundException("Error al reactivar y encontrar usuario: " + userInput));

            return PrincipalUser.build(reactivatedUser);
        }

        // 6. Si no se encuentra ni activo ni inactivo, lanza la excepción
        throw new UsernameNotFoundException("Usuario no encontrado: " + userInput);
    }

}
