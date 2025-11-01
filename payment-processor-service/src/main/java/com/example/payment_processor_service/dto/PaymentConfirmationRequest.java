package com.example.payment_processor_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmationRequest {
    private Long tuitionId;
    private String otpCode;
}