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
        return isAuthPath(servletPath) || isAuthPath(requestUri)
                || isGameWebSocketHandshake(request)
                || isPermitAllPublicApiPath(servletPath)
                || isPermitAllPublicApiPath(requestUri);
    }

    /**
     * Публичные эндпоинты из {@code SecurityConfig}: иначе при {@code credentials: 'include'}
     * и битом refresh-cookie на api-домене фильтр отвечает 401 до цепочки — браузер видит «CORS blocked».
     */
    private static boolean isPermitAllPublicApiPath(String raw) {
        if (raw == null || raw.isEmpty()) {
            return false;
        }
        String p = raw.startsWith("/") ? raw : "/" + raw;
        return switch (p) {
            case "/cases/get", "/api/cases/get",
                 "/sundry/get", "/api/sundry/get",
                 "/rate/get", "/api/rate/get",
                 "/main-news/get", "/api/main-news/get",
                 "/mini-news/get", "/api/mini-news/get",
                 "/rank/get", "/api/rank/get",
                 "/wiki/get", "/api/wiki/get",
                 "/payments/init", "/api/payments/init",
                 "/payments/tbank/notification", "/api/payments/tbank/notification",
                 "/game/online", "/api/game/online" -> true;
            default -> p.startsWith("/payments/status/") || p.startsWith("/api/payments/status/")
                    || p.startsWith("/swagger-ui")
                    || p.startsWith("/api/swagger-ui")
                    || p.startsWith("/v3/api-docs")
                    || p.startsWith("/api/v3/api-docs")
                    || "/swagger-ui.html".equals(p)
                    || "/api/swagger-ui.html".equals(p)
                    || p.startsWith("/webjars/")
                    || p.startsWith("/api/webjars/")
                    || p.startsWith("/files/")
                    || p.startsWith("/api/files/")
                    || p.startsWith("/actuator/")
                    || p.startsWith("/api/actuator/");
        };
    }

    /** Handshake WebSocket для плагина: без JWT, доступ по секрету в {@code GameServerWebSocketHandshakeInterceptor}. */
    private static boolean isGameWebSocketHandshake(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String servletPath = request.getServletPath() != null ? request.getServletPath() : "";
        String uri = request.getRequestURI() != null ? request.getRequestURI() : "";
        return isGameWsPath(servletPath) || isGameWsPath(uri);
    }

    private static boolean isGameWsPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String p = path.startsWith("/") ? path : "/" + path;
        return "/game/ws".equals(p) || "/api/game/ws".equals(p);
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
