package com.back.minecraftback.configuration;

import com.back.minecraftback.configuration.auth_exception.JwtAccessDeniedHandler;
import com.back.minecraftback.configuration.auth_exception.JwtAuthenticationEntryPoint;
import com.back.minecraftback.filter.JwtAuthFilter;
import com.back.minecraftback.service.AdminUsersDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final AdminUsersDetailService adminUserDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;

    /** Пути входа и refresh — по любому варианту пути (servletPath и requestURI). */
    private static final RequestMatcher AUTH_PATHS = (request) -> {
        String path = request.getServletPath();
        String uri = request.getRequestURI();
        if (path != null) {
            if (path.equals("/auth") || path.equals("/api/auth") || path.startsWith("/auth/") || path.startsWith("/api/auth/")) return true;
        }
        if (uri != null) {
            return uri.equals("/api/auth") || uri.endsWith("/auth") || uri.contains("/auth/");
        }
        return false;
    };

    @Bean
    @org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authOnlyFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AUTH_PATHS)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/auth", "/auth/**", "/api/auth", "/api/auth/**",
                                "/files/**", "/api/files/**",
                                "/actuator/**",
                                "/cases/get", "/api/cases/get",
                                "/rate/get", "/api/rate/get",
                                "/main-news/get", "/api/main-news/get",
                                "/mini-news/get", "/api/mini-news/get",
                                "/rank/get", "/api/rank/get",
                                "/wiki/get", "/api/wiki/get",
                                "/payments/init", "/api/payments/init",
                                "/payments/tbank/notification", "/api/payments/tbank/notification"
                        ).permitAll()
                        .requestMatchers(request -> {
                            String path = request.getServletPath();
                            String uri = request.getRequestURI();
                            return (path != null && (path.equals("/auth") || path.startsWith("/auth/") || path.equals("/api/auth") || path.startsWith("/api/auth/")))
                                    || (uri != null && (uri.endsWith("/auth") || uri.endsWith("/auth/") || uri.contains("/auth/")));
                        }).permitAll()
                        .requestMatchers(
                                "/admin/create", "/api/admin/create",
                                "/admin/enabled", "/api/admin/enabled",
                                "/admin", "/api/admin",
                                "/admin/data", "/api/admin/data",
                                "/admin/clear/**", "/api/admin/clear/**",
                                "/rank/clear", "/api/rank/clear",
                                "/cases/clear", "/api/cases/clear",
                                "/main-news/clear", "/api/main-news/clear",
                                "/mini-news/clear", "/api/mini-news/clear",
                                "/db/**", "/api/db/**"
                        ).hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE,
                                "/rank/*", "/api/rank/*",
                                "/cases/*", "/api/cases/*",
                                "/main-news/*", "/api/main-news/*",
                                "/mini-news/*", "/api/mini-news/*",
                                "/wiki/*", "/api/wiki/*",
                                "/admin/user/*", "/api/admin/user/*"
                        ).hasRole("SUPER_ADMIN")
                        .anyRequest().authenticated()
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(adminUserDetailsService)
                .passwordEncoder(passwordEncoder);
        return authBuilder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Только доверенные домены (без wildcard при allowCredentials)
        configuration.setAllowedOrigins(List.of(
                "https://night-vision.su",
                "http://night-vision.su",
                "https://www.night-vision.su",
                "http://www.night-vision.su",
                "https://api.night-vision.su",
                "http://api.night-vision.su"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}