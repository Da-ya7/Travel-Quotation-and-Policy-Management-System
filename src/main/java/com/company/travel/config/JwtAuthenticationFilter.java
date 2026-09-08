package com.company.travel.config;

import com.company.travel.auth.service.CustomUserDetailsService;
import com.company.travel.auth.service.JwtService;

import io.jsonwebtoken.Claims;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final CustomUserDetailsService userDetailsService;
        private final JwtService jwtService;

        public JwtAuthenticationFilter(
                        CustomUserDetailsService userDetailsService,
                        JwtService jwtService) {

                this.userDetailsService = userDetailsService;
                this.jwtService = jwtService;
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                // 1. Get the Authorization header
                String authHeader = request.getHeader("Authorization");

                // 2. If there is no Bearer token,
                // continue the request.
                if (authHeader == null ||
                                !authHeader.startsWith("Bearer ")) {

                        filterChain.doFilter(request, response);
                        return;
                }

                // 3. Extract the JWT
                String token = authHeader.substring(7);

                try {

                        // 4. Validate the JWT and extract its claims
                        Claims claims = jwtService.parseToken(token);

                        // 5. Get username from JWT subject
                        String username = claims.getSubject();

                        // 6. Make sure authentication hasn't already
                        // been created for this request
                        if (username != null &&
                                        SecurityContextHolder
                                                        .getContext()
                                                        .getAuthentication() == null) {

                                // 7. Load the user from the database
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                // 8. Create Spring Security authentication
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                userDetails.getAuthorities());

                                // 9. Attach request details
                                authentication.setDetails(
                                                new WebAuthenticationDetailsSource()
                                                                .buildDetails(request));

                                // 10. Store authentication in Spring Security
                                SecurityContextHolder
                                                .getContext()
                                                .setAuthentication(authentication);
                        }

                } catch (Exception exception) {

                        // Invalid/expired JWT or invalid user
                        // means the request is not authenticated.
                        SecurityContextHolder
                                        .clearContext();
                }

                // 11. Continue the request
                filterChain.doFilter(request, response);
        }
}