package com.wk.ti.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Arrays;

public class WithOidcUserSecurityContextFactory
        implements WithSecurityContextFactory<WithJwtUser> {

    @Override
    public @NonNull SecurityContext createSecurityContext(WithJwtUser annotation) {

        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(annotation.subject())
                .claim("email", annotation.email())
                .claim("given_name", annotation.givenName())
                .claim("family_name", annotation.familyName())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();

        var authorities = Arrays.stream(annotation.authorities())
                .map(SimpleGrantedAuthority::new)
                .toList();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt, authorities);

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        return context;
    }
}