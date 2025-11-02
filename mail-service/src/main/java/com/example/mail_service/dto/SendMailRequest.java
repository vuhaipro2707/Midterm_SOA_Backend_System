package com.example.mail_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMailRequest {
    private String to;
    private String subject;
    private String body;
}