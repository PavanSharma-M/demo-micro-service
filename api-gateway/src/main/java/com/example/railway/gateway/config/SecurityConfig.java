package com.example.railway.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Profile("!local")
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/**").permitAll()
                        // GET: any authenticated user
                        .pathMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("USER", "ADMIN")
                        // Bookings POST/PATCH: users and admins can book & cancel
                        .pathMatchers(HttpMethod.POST, "/api/v1/bookings/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/bookings/**").hasAnyRole("USER", "ADMIN")
                        // Orders POST/PATCH: users and admins can create and track shipment state
                        .pathMatchers(HttpMethod.POST, "/api/v1/orders/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/orders/**").hasAnyRole("USER", "ADMIN")
                        // Everything else (POST/PUT/DELETE on trains, stations, routes) → ADMIN only
                        .anyExchange().hasRole("ADMIN")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter()))
                )
                .build();
    }

    /**
     * Extracts Keycloak roles from realm_access.roles in the JWT and
     * maps them to Spring Security ROLE_* granted authorities.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakJwtConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) {
                return Flux.empty();
            }
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) {
                return Flux.empty();
            }
            List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return Flux.fromIterable(authorities);
        });
        return converter;
    }
}
