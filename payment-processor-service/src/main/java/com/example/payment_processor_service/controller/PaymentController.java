package com.example.payment_processor_service.controller;

import com.example.payment_processor_service.dto.GenericResponse;
import com.example.payment_processor_service.dto.PaymentRequest;
import com.example.payment_processor_service.model.PaymentTransaction;
import com.example.payment_processor_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/info")
    public ResponseEntity<GenericResponse<List<PaymentTransaction>>> getPaymentHistory(Authentication authentication) {
        Long customerId;
        try {
            customerId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        }

        try {
            List<PaymentTransaction> transactions = paymentService.getPaymentHistory(customerId);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.status(404).body(GenericResponse.failure("No payment transactions found for customer."));
            }
            
            return ResponseEntity.ok(GenericResponse.success("Payment transaction history retrieved successfully.", transactions));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error while fetching payment history: " + e.getMessage()));
        }
    }

    @PostMapping("/process")
    public ResponseEntity<GenericResponse<PaymentTransaction>> processPayment(@RequestBody PaymentRequest request, Authentication authentication) {
        Long customerId;
        try {
            customerId = Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        }

        if (request.getTuitionId() == null || request.getStudentId() == null || request.getAmount() == null || request.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: tuitionId, studentId, and a positive amount are required."));
        }
        
        try {
            PaymentTransaction transaction = paymentService.processPayment(customerId, request);
            return ResponseEntity.ok(GenericResponse.success("Payment successful and transaction recorded.", transaction));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error during payment: " + e.getMessage()));
        }
    }
}