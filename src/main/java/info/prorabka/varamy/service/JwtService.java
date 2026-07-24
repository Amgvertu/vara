package info.prorabka.varamy.service;

import info.prorabka.varamy.config.JwtConfig;
import info.prorabka.varamy.entity.RefreshToken;
import info.prorabka.varamy.entity.User;
import info.prorabka.varamy.repository.RefreshTokenRepository;
import info.prorabka.varamy.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                .claim("phone", user.getPhone())
                .claim("role", user.getRole())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    @Transactional
    public RefreshToken generateRefreshToken(User user, String userAgent, String ipAddress) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtConfig.getRefreshTokenExpiration()));
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);

        return refreshTokenRepository.save(refreshToken);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }
}
