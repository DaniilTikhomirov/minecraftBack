package com.back.minecraftback.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.back.minecraftback.model.TokenTime.JWT_TOKEN_TIME_IN_MILISECONDS;
import static com.back.minecraftback.model.TokenTime.REFRESH_TOKEN_TIME_IN_MILISECONDS;

@Component
public class JwtUtil {

    @Value("${jwt.secret.key}")
    private String SECRET_KEY_JWT;

    @Value("${refresh.secret.key}")
    private String SECRET_KEY_REFRESH;

    private final MacAlgorithm signatureAlgorithm = Jwts.SIG.HS256;

    private SecretKey getSigningKey(String key) {
        return Keys.hmacShaKeyFor(key.getBytes());
    }

    public String generateJwtToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), JWT_TOKEN_TIME_IN_MILISECONDS.getTime(), SECRET_KEY_JWT);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), REFRESH_TOKEN_TIME_IN_MILISECONDS.getTime(), SECRET_KEY_REFRESH);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration, String key) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(key), signatureAlgorithm)
                .compact();
    }

    public Boolean validateTokenJwt(String token, UserDetails userDetails) {
        final String username = extractUsernameJwt(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpiredJwt(token));
    }

    public Boolean validateTokenRefresh(String token, UserDetails userDetails) {
        final String username = extractUsernameRefresh(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpiredRefresh(token));
    }


    public String extractUsernameJwt(String token) {
        return extractClaim(token, Claims::getSubject, SECRET_KEY_JWT);
    }

    public String extractUsernameRefresh(String token) {
        return extractClaim(token, Claims::getSubject, SECRET_KEY_REFRESH);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, String key) {
        final Claims claims = extractAllClaims(token, key);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, String key) {
        return Jwts.parser()
                .verifyWith(getSigningKey(key))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpiredJwt(String token) {
        return extractExpirationJwt(token).before(new Date());
    }

    private Boolean isTokenExpiredRefresh(String token) {
        return extractExpirationRefresh(token).before(new Date());
    }

    private Date extractExpirationJwt(String token) {
        return extractClaim(token, Claims::getExpiration, SECRET_KEY_JWT);
    }

    private Date extractExpirationRefresh(String token) {
        return extractClaim(token, Claims::getExpiration, SECRET_KEY_REFRESH);
    }
}