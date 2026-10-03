package com.tutorhub.auth.security;

import com.tutorhub.auth.service.JwtService;
import com.tutorhub.user.entity.UserStatus;
import com.tutorhub.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String rawToken = authorization.substring(7);
            try {
                if (jwtService.validateToken(rawToken)) {
                    Long userId = jwtService.extractUserId(rawToken);
                    userRepository.findById(userId)
                        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                        .ifPresent(user -> {
                            UserPrincipal principal = UserPrincipal.from(user);
                            var authentication = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.authorities()
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}