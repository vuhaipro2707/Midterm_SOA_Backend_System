package com.example.customer_management_service.dto;

import lombok.Data;

@Data
public class DebitRequest {
    private Integer amount;
    private String description;
}