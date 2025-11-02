package com.example.mail_service.controller;

import com.example.mail_service.dto.GenericResponse;
import com.example.mail_service.dto.SendMailRequest;
import com.example.mail_service.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailController {

    @Autowired
    private MailService mailService;

    @PostMapping("/send")
    public ResponseEntity<GenericResponse<Void>> sendMail(@RequestBody SendMailRequest request) {
        if (request.getTo() == null || request.getSubject() == null || request.getBody() == null) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: 'to', 'subject', and 'body' are required."));
        }
        
        try {
            mailService.sendSimpleMail(request);
            return ResponseEntity.ok(GenericResponse.success("Email sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Failed to send email: " + e.getMessage()));
        }
    }
}