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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();
        String token = getToken(cookies, null, JWT_TOKEN.getToken());

        try {
            if (token != null) {
                handleJwtToken(token, request, response);
            } else {
                handleRefreshToken(request, response, cookies);
            }
        } catch (RuntimeException e) {
            // Ошибка токена или пользователя → выбрасываем наружу, чтобы GlobalExceptionHandler вернул 401/403
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleJwtToken(String token, HttpServletRequest request, HttpServletResponse response) {
        String username = jwtUtil.extractUsernameJwt(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Проверка токена
            if (!jwtUtil.validateTokenJwt(token, userDetails)) {
                throw new RuntimeException("Invalid JWT token");
            }

            // Проверка, что пользователь включен
            if (!userDetails.isEnabled()) {
                throw new RuntimeException("User is disabled");
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
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
                newCookie.setSecure(false); // true на продакшене
                newCookie.setPath("/");
                newCookie.setMaxAge(JWT_TOKEN_TIME_IN_SECONDS.getTime());
                response.addCookie(newCookie);

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
