package com.example.customer_management_service.controller;

import com.example.customer_management_service.dto.DebitRequest;
import com.example.customer_management_service.dto.GenericResponse;
import com.example.customer_management_service.service.CustomerService;
import com.example.customer_management_service.model.Customer; // Import Customer model
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional; 

@RestController
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping("/info")
    public ResponseEntity<GenericResponse<Customer>> getCustomerInfo(Authentication authentication) {
        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());

            Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setPassword(null); 
                return ResponseEntity.ok(GenericResponse.success("Customer info retrieved successfully.", customer));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        }
    }
    
    @GetMapping("/balance")
    public ResponseEntity<GenericResponse<Integer>> getAvailableBalance(Authentication authentication) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(GenericResponse.failure("User not authenticated."));
        }

        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());

            Integer balance = customerService.getAvailableBalance(customerId);

            if (balance != null) {
                return ResponseEntity.ok(GenericResponse.success("Current balance retrieved successfully.", balance));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Invalid customer ID format in authentication context."));
        }
    }
    
    @PostMapping("/balance/debit")
    public ResponseEntity<GenericResponse<Integer>> debitCustomerBalance(@RequestBody DebitRequest request, Authentication authentication) {

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: positive amount is required."));
        }

        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            Integer newBalance = customerService.debitCustomerBalance(customerId, request.getAmount());

            if (newBalance != null) {
                return ResponseEntity.ok(GenericResponse.success("Debit successful.", newBalance));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(GenericResponse.failure(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/balance/credit")
    public ResponseEntity<GenericResponse<Integer>> creditCustomerBalance(@RequestBody DebitRequest request, Authentication authentication) {

        if (request.getAmount() == null || request.getAmount() <= 0) {
            return ResponseEntity.badRequest().body(GenericResponse.failure("Invalid request: positive amount is required."));
        }

        try {
            Long customerId = Long.parseLong(authentication.getPrincipal().toString());
            Integer newBalance = customerService.creditCustomerBalance(customerId, request.getAmount());

            if (newBalance != null) {
                return ResponseEntity.ok(GenericResponse.success("Credit successful.", newBalance));
            } else {
                return ResponseEntity.status(404).body(GenericResponse.failure("Customer not found."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GenericResponse.failure("Internal server error: " + e.getMessage()));
        }
    }
}