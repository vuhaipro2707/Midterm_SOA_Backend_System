package com.example.otp_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpValidateRequest {
    private Long tuitionId;
    private String otpCode;
}