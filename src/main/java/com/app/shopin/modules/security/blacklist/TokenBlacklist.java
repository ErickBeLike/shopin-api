package com.app.shopin.modules.security.blacklist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class TokenBlacklist {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklist.class);

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void add(String token, long expirationEpochSeconds) {
        blacklist.put(token, expirationEpochSeconds);
    }

    public boolean contains(String token) {
        Long exp = blacklist.get(token);
        if (exp == null) return false;

        if (exp < Instant.now().getEpochSecond()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000) // cada hora
    //@Scheduled(fixedRate = 15 * 1000) // cada 5 minutos
    public void cleanExpiredTokens() {
        long now = Instant.now().getEpochSecond();
        int before = blacklist.size();

        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);

        int after = blacklist.size();
        int removed = before - after;

        if (removed > 0) {
            logger.info("🧹 Limpieza automática: {} token(s) expirados eliminados de la blacklist", removed);
        } else {
            logger.info("🧹 Limpieza automática ejecutada: ningún token expirado en este ciclo");
        }
    }
}
