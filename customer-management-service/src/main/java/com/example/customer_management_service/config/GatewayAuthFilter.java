package com.example.customer_management_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils; // Dùng để tạo role mặc định
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String CUSTOMER_ID_HEADER = "X-Customer-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String customerIdHeader = request.getHeader(CUSTOMER_ID_HEADER);
            if (StringUtils.hasText(customerIdHeader)) {
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        customerIdHeader,
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_USER") 
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Could not set user authentication in security context: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}