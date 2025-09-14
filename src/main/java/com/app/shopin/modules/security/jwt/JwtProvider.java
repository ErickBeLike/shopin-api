package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.accessToken.expiration}")
    private int accessTokenExpiration; // en segundos
    @Value("${jwt.refreshToken.expiration}")
    private int refreshTokenExpiration; // en segundos

    @Autowired
    private UserRepository userRepository;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("La clave debe tener al menos 256 bits");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un JWT que incluye:
     *  - sub (username)
     *  - roles
     *  - tv (tokenVersion de la entidad User)
     *  - iat / exp
     */
    public String generateAccessToken(Authentication authentication) {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();

        List<String> roles = principalUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", principalUser.getUsername());
        claims.put("roles", roles);
        claims.put("tv", principalUser.getTokenVersion());       // <-- tokenVersion
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(accessTokenExpiration).getEpochSecond());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshToken(Authentication authentication) {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", principalUser.getUsername());
        claims.put("iat", Instant.now().getEpochSecond());
        // --- Usa la expiración del refresh token ---
        claims.put("exp", Instant.now().plusSeconds(refreshTokenExpiration).getEpochSecond());

        return Jwts.builder().setClaims(claims).signWith(getSecretKey()).compact();
    }

    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = getAllClaimsFromToken(refreshToken);
            String username = claims.getSubject();

            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

            // Recreamos el objeto Authentication para generar un nuevo Access Token
            PrincipalUser principal = PrincipalUser.build(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());

            // Generamos y devolvemos un nuevo Access Token
            return generateAccessToken(authentication);

        } catch (ExpiredJwtException eje) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Refresh token expirado. Por favor, inicie sesión de nuevo.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Refresh token inválido.");
        }
    }

    public String generateAccessToken(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        // Para un usuario de Google, le asignamos el rol por defecto y tokenVersion 0.
        // Tu CustomOAuth2UserService ya se encarga de guardar esto en la BD.
        List<String> roles = List.of("ROLE_USER");
        Integer tokenVersion = 0;

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email); // Usamos el email como subject
        claims.put("roles", roles);
        claims.put("tv", tokenVersion);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(accessTokenExpiration).getEpochSecond());

        return Jwts.builder().setClaims(claims).signWith(getSecretKey()).compact();
    }

    /**
     * Genera un Refresh Token a partir de un usuario de OAuth2.
     */
    public String generateRefreshToken(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(refreshTokenExpiration).getEpochSecond());

        return Jwts.builder().setClaims(claims).signWith(getSecretKey()).compact();
    }

    /**
     * Extrae los claims aunque el token ya esté expirado.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getNombreUsuarioFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateTokenAndVersion(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Integer tokenVersionFromJwt = claims.get("tv", Integer.class);
            String username = claims.getSubject();

            if (tokenVersionFromJwt == null || username == null) {
                return false;
            }

            User user = userRepository.findByUserName(username)
                    .orElse(null);

            return user != null && tokenVersionFromJwt.equals(user.getTokenVersion());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationFromToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload().getExpiration().toInstant().getEpochSecond();
    }

    public String generateTokenWithExpiration(String username, long expirationInMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        // No añadimos roles ni tokenVersion a este token simple
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusMillis(expirationInMillis).getEpochSecond());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSecretKey())
                .compact();
    }
}