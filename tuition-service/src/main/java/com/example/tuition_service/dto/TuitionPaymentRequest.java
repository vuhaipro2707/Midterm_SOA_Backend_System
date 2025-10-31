package com.example.tuition_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuitionPaymentRequest {
    private Long tuitionId;
    private String studentId;
    private Boolean isPaid;
}