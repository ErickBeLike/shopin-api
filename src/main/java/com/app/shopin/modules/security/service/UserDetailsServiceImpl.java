package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.User;
import com.app.shopin.modules.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userInput) throws UsernameNotFoundException {
        User user = userRepository.findByUserNameOrEmail(userInput, userInput)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con nombre de usuario o correo: " + userInput));
        return PrincipalUser.build(user);
    }

}
