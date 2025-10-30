package com.example.customer_account_service.controller;

import com.example.customer_account_service.jwt.JwtTokenProvider;
import com.example.customer_account_service.dto.LoginRequest;
import com.example.customer_account_service.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie; 
import jakarta.servlet.http.HttpServletResponse; 


@RestController
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticateUser(@RequestBody LoginRequest loginRequest, 
                                             HttpServletResponse response) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

        } catch (BadCredentialsException ex) {
            return new ResponseEntity<>(
                new LoginResponse("Login failed", "Tên đăng nhập hoặc mật khẩu không đúng."),
                HttpStatus.UNAUTHORIZED 
            );
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        Cookie cookie = new Cookie("jwt_token", jwt);
        
        cookie.setHttpOnly(true); 
        // cookie.setSecure(true);
        cookie.setPath("/");      
        cookie.setMaxAge(86400); // 24 giờ
        
        response.addCookie(cookie);

        return ResponseEntity.ok(new LoginResponse("Login successful", "JWT token set in HttpOnly Cookie."));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logoutUser(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt_token", null); 
        cookie.setHttpOnly(true);
        // cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Xóa Cookie
        response.addCookie(cookie);
        
        return ResponseEntity.ok(new LoginResponse("Logout successful.", "Cookie cleared."));
    }
}