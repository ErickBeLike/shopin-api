package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.JwtDTO;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private int expiration; // en segundos

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
    public String generateToken(Authentication authentication) {
        PrincipalUser principalUser = (PrincipalUser) authentication.getPrincipal();

        List<String> roles = principalUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", principalUser.getUsername());
        claims.put("roles", roles);
        claims.put("tv", principalUser.getTokenVersion());       // <-- tokenVersion
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(expiration).getEpochSecond());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSecretKey())
                .compact();
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

    /**
     * Si el token expiró, intenta refrescarlo:
     *  - Parsea los claims
     *  - Compara el 'tv' en el token con el tokenVersion actual en BD
     *  - Si coincide, renueva iat/exp y devuelve un token nuevo
     *  - Si no coincide, lanza excepción para obligar re-login
     */
    public String refreshToken(JwtDTO jwtDto) throws ParseException {
        try {
            // si NO está expirado, no refrescamos
            getAllClaimsFromToken(jwtDto.getToken());
            return null;
        } catch (ExpiredJwtException eje) {
            Claims claims = eje.getClaims();

            // 1) extraer userId o username
            String username = claims.getSubject();
            Integer tvInToken = claims.get("tv", Integer.class);

            // 2) leer usuario de BD
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new CustomException(
                            HttpStatus.UNAUTHORIZED,
                            "Usuario no encontrado"
                    ));

            // 3) comparar versiones
            if (!tvInToken.equals(user.getTokenVersion())) {
                throw new CustomException(
                        HttpStatus.UNAUTHORIZED,
                        "Sus credenciales cambiaron. Vuelva a iniciar sesión."
                );
            }

            // 4) renovar fechas
            Instant now = Instant.now();
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", now.plusSeconds(expiration).getEpochSecond());
            // 'tv' permanece igual en claims

            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(getSecretKey())
                    .compact();
        }
    }

    public long getExpirationFromToken(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
        return jws.getPayload().getExpiration().toInstant().getEpochSecond();
    }
}