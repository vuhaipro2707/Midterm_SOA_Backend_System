package com.example.customer_account_service.jwt;

import com.example.customer_account_service.model.Customer;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;
    
    // ResourceLoader để tải Private Key từ resources
    private final ResourceLoader resourceLoader;

    public JwtTokenProvider(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // Phương thức tải Private Key từ file
    private PrivateKey getPrivateKey() {
        try {
            InputStream inputStream = resourceLoader.getResource("classpath:keys/private_key.pem").getInputStream();
            String keyString = new String(inputStream.readAllBytes());
            
            keyString = keyString
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Error loading Private Key for JWT signing", e);
        }
    }

    public String generateToken(Authentication authentication) {
        Customer customerPrincipal = (Customer) authentication.getPrincipal(); 
        PrivateKey privateKey = getPrivateKey();

        String customerId = customerPrincipal.getCustomerId().toString(); 
        String customerFullName = customerPrincipal.getFullName(); 

        return Jwts.builder()
                .setSubject(customerPrincipal.getUsername())
                .claim("customerId", customerId) 
                .claim("fullName", customerFullName) 
                .claim("roles", customerPrincipal.getAuthorities()) 
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}