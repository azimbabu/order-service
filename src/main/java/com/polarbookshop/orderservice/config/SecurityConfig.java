package com.polarbookshop.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain webFilterChain(ServerHttpSecurity httpSecurity) {
    return httpSecurity.authorizeExchange(
            exchangeSpec -> exchangeSpec
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated())
        //.oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt)
        .oauth2ResourceServer(
            oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec.jwt(Customizer.withDefaults()))
        .requestCache(
            requestCacheSpec -> requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
  }
}
