package com.back.minecraftback.filter;

import com.back.minecraftback.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static com.back.minecraftback.model.Token.JWT_TOKEN;
import static com.back.minecraftback.model.Token.REFRESH_TOKEN;
import static com.back.minecraftback.model.TokenTime.JWT_TOKEN_TIME_IN_SECONDS;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /** Не проверять JWT для входа и обновления токена — там токена ещё нет или он в теле. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath() != null ? request.getServletPath() : "";
        String requestUri = request.getRequestURI() != null ? request.getRequestURI() : "";
        return isAuthPath(servletPath) || isAuthPath(requestUri);
    }

    private static boolean isAuthPath(String path) {
        if (path == null || path.isEmpty()) return false;
        String p = path.startsWith("/") ? path : "/" + path;
        return p.equals("/auth")
                || p.startsWith("/auth/")
                || p.equals("/api/auth")
                || p.startsWith("/api/auth/")
                || p.endsWith("/auth")
                || p.endsWith("/api/auth")
                || p.contains("/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();
        String token = getToken(cookies, null, JWT_TOKEN.getToken());

        try {
            if (token != null) {
                boolean jwtAuthenticated = handleJwtToken(token, request);
                if (!jwtAuthenticated) {
                    handleRefreshToken(request, response, cookies);
                }
            } else {
                handleRefreshToken(request, response, cookies);
            }
        } catch (RuntimeException e) {
            // Ошибка токена/пользователя: запрос не авторизован.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean handleJwtToken(String token, HttpServletRequest request) {
        String username;
        try {
            username = jwtUtil.extractUsernameJwt(token);
        } catch (RuntimeException ex) {
            // Невалидный/просроченный access token: пробуем продолжить через refresh token.
            return false;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Проверка токена
            if (!jwtUtil.validateTokenJwt(token, userDetails)) {
                return false;
            }

            // Проверка, что пользователь включен
            if (!userDetails.isEnabled()) {
                throw new RuntimeException("User is disabled");
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            return true;
        }
        return false;
    }

    private void handleRefreshToken(HttpServletRequest request, HttpServletResponse response, Cookie[] cookies) {
        String refreshToken = getToken(cookies, null, REFRESH_TOKEN.getToken());

        if (refreshToken != null) {
            String username = jwtUtil.extractUsernameRefresh(refreshToken);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!jwtUtil.validateTokenRefresh(refreshToken, userDetails)) {
                    throw new RuntimeException("Invalid Refresh token");
                }

                // Проверка, что пользователь включен
                if (!userDetails.isEnabled()) {
                    throw new RuntimeException("User is disabled");
                }

                // Генерируем новый JWT
                String newJwt = jwtUtil.generateJwtToken(userDetails);
                Cookie newCookie = new Cookie(JWT_TOKEN.getToken(), newJwt);
                newCookie.setHttpOnly(true);
                newCookie.setSecure(true);
                newCookie.setPath("/");
                newCookie.setMaxAge(JWT_TOKEN_TIME_IN_SECONDS.getTime());
                response.addCookie(newCookie);
                response.addHeader("Set-Cookie",
                        JWT_TOKEN.getToken() + "=" + newJwt + "; Path=/; Secure; HttpOnly; SameSite=None");

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
    }

    private static String getToken(Cookie[] cookies, String jwt, String jwtName) {
        if (cookies != null) {
            jwt = Arrays.stream(cookies)
                    .filter(cookie -> jwtName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return jwt;
    }
}
