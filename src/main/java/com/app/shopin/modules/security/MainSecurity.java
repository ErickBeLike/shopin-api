package com.app.shopin.modules.security;

import com.app.shopin.modules.security.jwt.JwtEntryPoint;
import com.app.shopin.modules.security.jwt.JwtTokenFilter;
import com.app.shopin.modules.security.jwt.OAuth2LoginSuccessHandler;
import com.app.shopin.modules.security.service.CustomOAuth2UserService;
import com.app.shopin.modules.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class MainSecurity {

    @Autowired
    UserDetailsServiceImpl userDetailsServiceImpl;

    @Autowired
    JwtEntryPoint jwtEntryPoint;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenFilter jwtTokenFilter;

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    AuthenticationManager authenticationManager;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsServiceImpl).passwordEncoder(passwordEncoder);
        return builder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // --- INICIO DE LAS REGLAS DE ACCESO COMPLETAS ---
                .authorizeHttpRequests(auth -> auth
                        // 1. Rutas Públicas (no requieren token)
                        .requestMatchers("/api/auth/**", "/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/api/email/send").permitAll() // <-- Faltaba esta

                        // 2. Rutas Específicas por Rol
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN") // <-- Faltaba esta
                        .requestMatchers("/api/employee/**").hasAnyRole("ADMIN", "SUPERADMIN", "EMPLOYEE") // <-- Faltaba esta

                        // 3. Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                // --- FIN DE LAS REGLAS DE ACCESO ---

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )

                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .exceptionHandling(exc -> exc.authenticationEntryPoint(jwtEntryPoint))
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
