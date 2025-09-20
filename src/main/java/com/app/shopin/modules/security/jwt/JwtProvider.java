package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.oauth2.OAuth2TempInfo;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
                .map(GrantedAuthority::getAuthority)
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
            String email = claims.getSubject();

            User user = userRepository.findByEmail(email)
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

    public String getEmailFromToken(String token) {
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
            String email = claims.getSubject();

            if (tokenVersionFromJwt == null || email == null) {
                return false;
            }

            User user = userRepository.findByEmail(email)
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

    public String generateTokenWithExpiration(String email, long expirationInMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", email);
        // No añadimos roles ni tokenVersion a este token simple
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusMillis(expirationInMillis).getEpochSecond());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRegistrationToken(OAuth2TempInfo tempInfo) {
        Map<String, Object> claims = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> tempInfoMap = mapper.convertValue(tempInfo, Map.class);

        claims.put("registration_info", tempInfoMap);
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond()); // Válido por 10 minutos

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSecretKey())
                .compact();
    }

    public OAuth2TempInfo getRegistrationInfoFromToken(String token) { // ESTE AHORA NO SE ESTÁ USANDO
        try {
            Claims claims = getAllClaimsFromToken(token);
            Map<String, Object> tempInfoMap = (Map<String, Object>) claims.get("registration_info");
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(tempInfoMap, OAuth2TempInfo.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Token de registro inválido o expirado.");
        }
    }
}