package com.mycompany.myshop.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Server-side OIDC login for the UI (authorization code, session cookie; tokens never reach browser JS) plus
 * Bearer-JWT validation for API clients. Both resolve to the same role rules.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String REGISTRATION_ID = "keycloak";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${auth.issuer-uri}")
    private String issuerUri;

    @Value("${auth.authorization-uri}")
    private String authorizationUri;

    @Value("${auth.token-uri}")
    private String tokenUri;

    @Value("${auth.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${auth.end-session-uri}")
    private String endSessionUri;

    @Value("${auth.audience}")
    private String audience;

    @Value("${auth.client-id}")
    private String clientId;

    @Value("${auth.client-secret}")
    private String clientSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemDetailSecurityHandlers handlers,
            ClientRegistrationRepository clientRegistrations) throws Exception {
        RequestMatcher apiRequests = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        RequestMatcher bearerRequests = request -> {
            var header = request.getHeader(HttpHeaders.AUTHORIZATION);
            return header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
        };

        http
            // Browser sessions need CSRF protection; Bearer clients carry no ambient credentials, so they are exempt.
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(bearerRequests))
            .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/orders/{orderNumber}/deliver").hasRole(ADMIN)
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole(CUSTOMER)
                .requestMatchers(HttpMethod.POST, "/api/coupons").hasRole(ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/coupons").hasRole(ADMIN)
                .requestMatchers("/api/admin/**").hasRole(ADMIN)
                .requestMatchers("/admin-coupons").hasRole(ADMIN)
                .requestMatchers("/new-order").hasRole(CUSTOMER)
                .anyRequest().authenticated())
            .oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(pkceAuthorizationRequestResolver(clientRegistrations)))
                .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(realmRolesMapper())))
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(handlers)
                .accessDeniedHandler(handlers))
            .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler(clientRegistrations)))
            .exceptionHandling(e -> e
                // API calls get 401 problem details; page requests keep the default redirect to the Keycloak login.
                .defaultAuthenticationEntryPointFor(handlers, new OrRequestMatcher(apiRequests, bearerRequests))
                // Everything else redirects to login regardless of the Accept header (clients without one, e.g. probes, would otherwise get a 401).
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"),
                    AnyRequestMatcher.INSTANCE)
                .accessDeniedHandler(handlers));
        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Built from explicit endpoints (no discovery): the browser-facing and server-to-server URIs differ
        // inside Docker, so the issuer's discovery document cannot be fetched from the container.
        ClientRegistration registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri(authorizationUri)
            .tokenUri(tokenUri)
            .jwkSetUri(jwkSetUri)
            .issuerUri(issuerUri)
            .userNameAttributeName("preferred_username")
            .providerConfigurationMetadata(Map.of("end_session_endpoint", endSessionUri))
            .clientName("Keycloak")
            .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    /** Adds PKCE (S256) to the authorization-code request; the realm client requires it. */
    private static OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ClientRegistrationRepository repository) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    private OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler(ClientRegistrationRepository repository) {
        var handler = new OidcClientInitiatedLogoutSuccessHandler(repository);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
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
        return jwt -> toAuthorities(jwt.getClaim("realm_access"));
    }

    /** Maps the realm roles carried in the ID token to ROLE_* authorities for the browser session. */
    private GrantedAuthoritiesMapper realmRolesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>(authorities);
            authorities.stream()
                .filter(OidcUserAuthority.class::isInstance)
                .map(OidcUserAuthority.class::cast)
                .forEach(a -> mapped.addAll(toAuthorities(a.getIdToken().getClaim("realm_access"))));
            return mapped;
        };
    }

    private static List<GrantedAuthority> toAuthorities(Object realmAccess) {
        if (!(realmAccess instanceof Map<?, ?> map) || !(map.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }

    /** Forces the deferred CSRF token to load so the XSRF-TOKEN cookie is set for the pages' fetch() calls. */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
