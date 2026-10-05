package com.mycompany.myshop.backend.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String CUSTOMER = "CUSTOMER";

    @Value("${auth.issuer-uri}")
    private String issuerUri;

    @Value("${auth.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${auth.audience}")
    private String audience;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandlers handlers)
            throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/{orderNumber}/deliver").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole(CUSTOMER)
                .requestMatchers(HttpMethod.POST, "/api/coupons").hasRole(ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/coupons").hasRole(ADMIN)
                .requestMatchers("/api/admin/**").hasRole(ADMIN)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(handlers)
                .accessDeniedHandler(handlers))
            .exceptionHandling(e -> e
                .authenticationEntryPoint(handlers)
                .accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
            "aud", aud -> aud != null && aud.contains(audience));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(realmRolesConverter());
        return converter;
    }

    private Converter<Jwt, Collection<GrantedAuthority>> realmRolesConverter() {
        return jwt -> {
            Object realmAccess = jwt.getClaim("realm_access");
            if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        };
    }
}
