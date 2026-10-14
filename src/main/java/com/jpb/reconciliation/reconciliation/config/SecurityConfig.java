package com.jpb.reconciliation.reconciliation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.jpb.reconciliation.reconciliation.security.JwtAuthenticationEntryPoint;
import com.jpb.reconciliation.reconciliation.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint point;

    @Autowired
    private JwtAuthenticationFilter filter;

    @Autowired
    OAuthAuthenticationSuccessHandler handler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth

                // Swagger
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()

                // Auth APIs
                .requestMatchers(new AntPathRequestMatcher("/auth/refresh-token")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/forgot-password")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/reset-password")).permitAll()

                // V2 Auth — login, refresh, logout, /me (all JWT-free login flows)
                .requestMatchers(new AntPathRequestMatcher("/api/v2/auth/**")).permitAll()

                // User APIs
                // /api/v2/user/create is NOT public: only an Admin or a Maker creates users, and the
                // service keys parentUserId, the REQUEST-vs-PENDING_APPROVAL decision and the
                // maker-checker audit row off the authenticated caller.
                .requestMatchers(new AntPathRequestMatcher("/api/v2/user/create-user")).permitAll()

                // User Auth (onboarding + login flow — no JWT required)
                .requestMatchers(new AntPathRequestMatcher("/api/v1/user/auth/**")).permitAll()

                // Google OAuth
                .requestMatchers(new AntPathRequestMatcher("/authentication/app")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/auth/google")).permitAll()

                // Kalinfotech APIs
                .requestMatchers(new AntPathRequestMatcher("/api/kalinfotech/**")).permitAll()

                // KalAdmin V2 Auth — create is public (no token needed for self-registration)
                .requestMatchers(new AntPathRequestMatcher("/api/v2/admin/auth/create")).permitAll()

                // Product Master — read-only list is public (used on bank onboarding form)
                .requestMatchers(new AntPathRequestMatcher("/api/v2/product/get-all")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v2/product/get-by-status")).permitAll()

                // H2
                .requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll()

                // V2 logo + get-by-code — public
                .requestMatchers(new AntPathRequestMatcher("/api/v2/bank/logo/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v2/bank/get-by-code/**")).permitAll()
                
                // ✅ IMPORTANT FIX FOR OTP VERIFY
                .requestMatchers(new AntPathRequestMatcher("/api/otp/**")).permitAll()


                .anyRequest().authenticated()
            )

            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(point)
            )

            // Without this, a request with no/invalid JWT (missing, malformed, expired) falls
            // through JwtAuthenticationFilter with SecurityContext empty, and Spring's
            // AnonymousAuthenticationFilter then fills in an anonymous principal. .anyRequest()
            // .authenticated() correctly rejects that anonymous principal, but does so via
            // AccessDeniedException -> a bare 403 from Spring's default handler, bypassing our
            // JwtAuthenticationEntryPoint entirely. The frontend only auto-logs-out on 401, so a
            // stale/expired session silently showed a broken "no menus assigned" screen instead of
            // signing the user out. Disabling anonymous auth means no-auth now raises an
            // AuthenticationException, which IS routed through the entry point -> a clean 401.
            .anonymous(anonymous -> anonymous.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .httpBasic(Customizer.withDefaults())

            .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)

            .headers(headers ->
                headers.frameOptions(frameOptions ->
                    frameOptions.sameOrigin()
                )
            );

        // OAuth Login
        http.oauth2Login(oauth -> {
            oauth.successHandler(handler);
        });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowCredentials(true);

        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("http://localhost:3000");

        configuration.addAllowedOrigin("http://80.225.233.60");

        configuration.addAllowedOrigin("http://13.48.46.135");
        configuration.addAllowedOrigin("http://13.48.46.135:8080");
        configuration.addAllowedOrigin("http://13.48.46.135:8081");

        configuration.addAllowedOrigin("https://jpbreconsit.jiopaymentsbank.com:8080");

        configuration.addAllowedOrigin("http://10.142.12.140:8080");
        configuration.addAllowedOrigin("http://192.168.1.103:8081");

        configuration.addAllowedOrigin("https://jio-recon.jiopaymentsbank.com:8080");
        configuration.addAllowedOrigin("https://jiorecon.jiopaymentsbank.com:8080");

        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}