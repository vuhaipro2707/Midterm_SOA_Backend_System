package com.example.otp_service.controller;

import com.example.otp_service.dto.GenericResponse;
import com.example.otp_service.dto.OtpGenerateRequest;
import com.example.otp_service.dto.OtpValidateRequest;
import com.example.otp_service.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class OtpController {

    @Autowired
    private OtpService otpService;

    private static final String CUSTOMER_ID_HEADER = "X-Customer-Id";
    
    private Long getCustomerId(String customerIdHeader) {
        if (customerIdHeader == null) {
            throw new IllegalArgumentException("Missing required header: " + CUSTOMER_ID_HEADER);
        }
        try {
            return Long.parseLong(customerIdHeader);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for header: " + CUSTOMER_ID_HEADER);
        }
    }
    
    @PostMapping("/generate")
    public ResponseEntity<GenericResponse<Map<String, String>>> generateOtp(
        @RequestHeader(CUSTOMER_ID_HEADER) String customerIdHeader,
        @RequestBody OtpGenerateRequest request) 
    {
        if (request.getTuitionId() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: tuitionId is required."));
        }
        
        try {
            Long customerId = getCustomerId(customerIdHeader);

            OtpService.OtpResult result = otpService.getOrCreateOtp(customerId, request.getTuitionId(), false);

            Map<String, String> data = Map.of(
                "otpCode", result.otpCode,
                "statusMessage", result.message 
            );

            return ResponseEntity.ok(GenericResponse.success(result.message, data));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<GenericResponse<Map<String, String>>> resendOtp(
        @RequestHeader(CUSTOMER_ID_HEADER) String customerIdHeader,
        @RequestBody OtpGenerateRequest request) 
    {
        if (request.getTuitionId() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: tuitionId is required."));
        }
        
        try {
            Long customerId = getCustomerId(customerIdHeader);
            
            OtpService.OtpResult result = otpService.getOrCreateOtp(customerId, request.getTuitionId(), true);

            Map<String, String> data = Map.of(
                "otpCode", result.otpCode,
                "statusMessage", result.message 
            );

            return ResponseEntity.ok(GenericResponse.success(result.message, data));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    
    @PostMapping("/validate")
    public ResponseEntity<GenericResponse<Object>> validateOtp(
        @RequestHeader(CUSTOMER_ID_HEADER) String customerIdHeader,
        @RequestBody OtpValidateRequest request) 
    {
        if (request.getTuitionId() == null || request.getOtpCode() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: tuitionId and otpCode are required."));
        }
        
        try {
            Long customerId = getCustomerId(customerIdHeader);

            boolean isValid = otpService.validateOtp(customerId, request.getTuitionId(), request.getOtpCode());

            if (isValid) {
                return ResponseEntity.ok(GenericResponse.success("OTP validated successfully."));
            } else {
                return ResponseEntity.status(401).body(GenericResponse.failure("Invalid or expired OTP."));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
}